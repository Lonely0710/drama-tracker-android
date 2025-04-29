package com.lonely.dramatracker.services

import android.content.Context
import android.util.Log
import com.lonely.dramatracker.config.AppConfig
import com.lonely.dramatracker.models.SearchResult
import io.appwrite.Client
import io.appwrite.ID
import io.appwrite.Query
import io.appwrite.models.Session
import io.appwrite.models.User
import io.appwrite.services.Account
import io.appwrite.services.Databases
import io.appwrite.services.Storage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Appwrite 服务封装类
 * 负责与Appwrite后端服务交互，包括用户认证、数据读写和文件存储等功能
 */
object Appwrite {
    lateinit var client: Client
    lateinit var account: Account
    lateinit var databases: Databases
    lateinit var storage: Storage

    // 使用自定义协程作用域替代GlobalScope
    private val appwriteScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // 标记是否已初始化
    private var isInitialized = false

    // 最大重试次数
    private const val MAX_RETRIES = 3

    // 重试延迟时间（毫秒）
    private const val RETRY_DELAY = 1000L

    /**
     * 检查Appwrite服务是否已初始化
     * @throws IllegalStateException 如果服务尚未初始化
     */
    private fun checkInitialized() {
        if (!isInitialized) {
            throw IllegalStateException("Appwrite服务尚未初始化，请先调用Appwrite.init(context)")
        }
    }

    /**
     * 取消所有正在进行的网络请求
     */
    fun cancelAllRequests() {
        appwriteScope.coroutineContext.cancelChildren()
    }

    /**
     * 初始化Appwrite客户端
     * @param context 应用上下文
     * @throws Exception 初始化失败时抛出异常
     */
    fun init(context: Context) {
        try {
            client = Client(context)
                .setEndpoint("https://cloud.appwrite.io/v1")
                .setProject(AppConfig.PROJECT_ID)
                .setSelfSigned(true) // 允许自签名证书

            account = Account(client)
            databases = Databases(client)
            storage = Storage(client)

            isInitialized = true
        } catch (e: Exception) {
            throw e
        }
    }

    //===================================
    // 协程方式 API - 适用于 Kotlin 调用
    //===================================

    // 创建用户文档的独立方法
    private suspend fun createUser(email: String, name: String, userId: String, avatarUrl: String): String {
        var retryCount = 0
        var lastException: Exception? = null

        while (retryCount < MAX_RETRIES) {
            try {
                checkInitialized()

                val userData = mapOf(
                    "user_id" to userId,
                    "email" to email,
                    "name" to name,
                    "avatar_url" to avatarUrl
                )

                val document = databases.createDocument(
                    AppConfig.DATABASE_ID,
                    AppConfig.COLLECTION_USERS_ID,
                    ID.unique(),  // 使用唯一ID作为文档ID，而不是userId
                    userData,
                )

                return document.id
            } catch (e: Exception) {
                lastException = e
                if (retryCount < MAX_RETRIES - 1) {
                    kotlinx.coroutines.delay(RETRY_DELAY)
                }
                retryCount++
            }
        }

        throw lastException ?: Exception("创建用户文档失败，已达到最大重试次数")
    }

    // 注册方法 - 协程版本
    suspend fun register(email: String, password: String, name: String): String {
        return withContext(Dispatchers.IO) {
            try {
                checkInitialized()

                val user = account.create(
                    ID.unique(), // 使用随机ID
                    email,
                    password,
                    name
                )
                val userId = user.id

                // 生成头像URL
                val avatarUrl = "https://ui-avatars.com/api/?name=${name.replace(" ", "+")}"

                // 2. 创建用户文档
                val documentId = createUser(email, name, userId, avatarUrl)

                // 3. 登录
                try {
                    account.createEmailPasswordSession(email, password)
                } catch (e: Exception) {
                    // 忽略登录失败，不影响注册结果
                }

                userId // 返回用户ID
            } catch (e: Exception) {
                throw e
            }
        }
    }

    // 登录 - 协程版本
    suspend fun login(email: String, password: String): Session {
        return withContext(Dispatchers.IO) {
            try {
                checkInitialized()

                val session = account.createEmailPasswordSession(email, password)
                session
            } catch (e: Exception) {
                throw e
            }
        }
    }

    // 获取当前用户 - 协程版本
    suspend fun getCurrentUser(): Map<String, Any>? {
        return withContext(Dispatchers.IO) {
            try {
                checkInitialized()

                // 获取当前会话用户
                val currentUser = account.get()

                // 确保用户存在
                if (currentUser.id != null) {
                    // 查询用户文档以获取更多信息
                    val response = databases.listDocuments(
                        AppConfig.DATABASE_ID,
                        AppConfig.COLLECTION_USERS_ID,
                        listOf(
                            Query.equal("user_id", currentUser.id)
                        )
                    )

                    if (response.documents.isNotEmpty()) {
                        val userDoc = response.documents[0]

                        val userData = userDoc.data.toMutableMap()

                        // 添加从账户中获取的其他字段
                        userData["name"] = currentUser.name
                        userData["email"] = currentUser.email

                        // 确保头像URL正确，检查原始字段存在
                        val avatarUrl = userData["avatar_url"] as? String

                        // 如果原始URL为空，生成一个更安全的头像URL
                        if (avatarUrl.isNullOrEmpty()) {
                            val encodedName = URLEncoder.encode(currentUser.name, "UTF-8")
                            // 使用更多参数，指定格式为.png
                            val defaultAvatarUrl = "https://ui-avatars.com/api/?name=$encodedName&size=200&background=random&format=png&rounded=true"
                            userData["avatarUrl"] = defaultAvatarUrl
                        } else {
                            userData["avatarUrl"] = avatarUrl
                        }

                        return@withContext userData
                    } else {
                        // 找不到用户文档时，生成默认头像并返回基本信息
                        val defaultAvatarUrl = "https://ui-avatars.com/api/?name=${currentUser.name.replace(" ", "+")}"
                        val basicData = mapOf(
                            "name" to currentUser.name,
                            "email" to currentUser.email,
                            "avatarUrl" to defaultAvatarUrl
                        )
                        return@withContext basicData
                    }
                }

                // 没有当前用户时返回 null
                null
            } catch (e: Exception) {
                null
            }
        }
    }


    // 登出 - 协程版本
    suspend fun logout(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                checkInitialized()

                account.deleteSession("current")
                true
            } catch (e: Exception) {
                false
            }
        }
    }

    // 获取当前用户ID的方法
    fun getCurrentUserId(): String {
        var userId = ""
        kotlinx.coroutines.runBlocking {
            try {
                checkInitialized()

                val session = account.get()
                userId = session.id
            } catch (e: Exception) {
                // 忽略错误，返回空字符串
            }
        }
        return userId
    }

    suspend fun isSourceIdCollected(sourceId: String): Boolean {
        checkInitialized()
        val dbId = AppConfig.DATABASE_ID
        val mediaSourceColId = AppConfig.COLLECTION_MEDIA_SOURCE_ID
        val query = listOf(Query.equal("source_id", sourceId))
        val result = databases.listDocuments(dbId, mediaSourceColId, query)
        return result.documents.isNotEmpty()
    }

    /**
     * 查询文档列表
     * @param databaseId 数据库ID
     * @param collectionId 集合ID
     * @param queries 查询列表
     * @param onSuccess 成功回调
     * @param onError 失败回调
     */
    fun listDocuments(
        databaseId: String,
        collectionId: String,
        queries: List<Query>,
        onSuccess: (List<Map<String, Any>>) -> Unit,
        onError: (Exception) -> Unit
    ) {
        appwriteScope.launch {
            try {
                checkInitialized()

                // 转换Query对象为字符串列表
                val queryStrings = queries.map { it.toString() }

                val documents = databases.listDocuments(
                    databaseId,
                    collectionId,
                    queries = queryStrings
                )

                withContext(Dispatchers.Main) {
                    onSuccess(documents.documents.map { it.data })
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onError(e)
                }
            }
        }
    }

    /**
     * 执行Kotlin协程函数的辅助方法，供Java代码调用
     * @param block 要执行的协程代码块
     */
    fun runBlocking(block: () -> Unit) {
        kotlinx.coroutines.runBlocking {
            block()
        }
    }

    //===================================
    // 回调方式 API - 适用于 Java 调用
    //===================================

    /**
     * 用户登录
     * @param email 用户邮箱
     * @param password 用户密码
     * @param onSuccess 登录成功回调
     * @param onError 登录失败回调
     */
    fun loginWithCallback(email: String, password: String, onSuccess: (Session) -> Unit, onError: (Exception) -> Unit) {
        appwriteScope.launch {
            try {
                checkInitialized()

                // 清除现有会话
                try {
                    account.deleteSession("current")
                } catch (e: Exception) {
                    // 忽略错误，可能没有现有会话
                }

                val session = account.createEmailPasswordSession(email, password)

                withContext(Dispatchers.Main) {
                    onSuccess(session)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onError(e)
                }
            }
        }
    }

    // 注册 - 回调版本
    fun registerWithCallback(email: String, password: String, name: String, onSuccess: (User<Map<String, Any>>) -> Unit, onError: (Exception) -> Unit) {
        appwriteScope.launch {
            try {
                checkInitialized()

                // 1. 创建用户账号
                val userId = ID.unique()

                // 先尝试删除现有会话
                try {
                    account.deleteSession("current")
                } catch (e: Exception) {
                    // 忽略删除会话时的错误，可能本来就没有会话
                }

                val user = account.create(
                    userId,
                    email,
                    password,
                    name
                )

                // 2. 创建用户文档
                val avatarUrl = "https://ui-avatars.com/api/?name=${name.replace(" ", "+")}"
                createUser(email, name, userId, avatarUrl)

                // 3. 登录
                try {
                    account.createEmailPasswordSession(email, password)
                } catch (e: Exception) {
                    // 忽略登录失败，不影响注册结果
                }

                // 返回结果
                withContext(Dispatchers.Main) {
                    onSuccess(user)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onError(e)
                }
            }
        }
    }

    // 获取当前用户 - Java 回调版本
    fun getCurrentUserWithCallback(onSuccess: (Map<String, Any>) -> Unit, onError: (Exception) -> Unit) {
        appwriteScope.launch {
            try {
                checkInitialized()

                val userData = getCurrentUser()
                if (userData != null) {
                    withContext(Dispatchers.Main) {
                        onSuccess(userData)
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        onError(Exception("用户信息为空"))
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onError(e)
                }
            }
        }
    }

    // 登出 - 回调版本
    fun logoutWithCallback(onSuccess: () -> Unit, onError: (Exception) -> Unit) {
        appwriteScope.launch {
            try {
                checkInitialized()

                account.deleteSession("current")

                withContext(Dispatchers.Main) {
                    onSuccess()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onError(e)
                }
            }
        }
    }

    // Java可调用的三表插入方法
    fun addMediaWithSourceAndCollection(result: SearchResult, userId: String, callback: (Boolean) -> Unit) {
        appwriteScope.launch {
            try {
                checkInitialized()
                val dbId = AppConfig.DATABASE_ID
                val mediaColId = AppConfig.COLLECTION_MEDIA_ID
                val mediaSourceColId = AppConfig.COLLECTION_MEDIA_SOURCE_ID
                val collectionColId = AppConfig.COLLECTION_COLLECTIONS_ID

                // 1. 检查media是否存在
                var mediaId: String
                val mediaQuery = listOf(
                    Query.equal("title_zh", result.titleZh),
                    Query.equal("release_date", result.releaseDate)
                )
                val mediaList = databases.listDocuments(
                    dbId, mediaColId, mediaQuery
                ).documents

                if (mediaList.isNotEmpty()) {
                    // 如果media已存在，使用现有ID
                    mediaId = mediaList[0].id
                } else {
                    // 如果media不存在，创建新记录
                    // 构造 mediaData，所有非法值用 null
                    val mediaData = mutableMapOf<String, Any?>(
                        "media_type" to result.mediaType,
                        "title_zh" to result.titleZh,
                        "title_origin" to result.titleOriginal,
                        "release_date" to result.releaseDate,
                        "poster_url" to result.posterUrl
                    )
                    if (!result.duration.isNullOrBlank() && result.duration != "null") mediaData["duration"] = result.duration else mediaData["duration"] = null
                    if (!result.summary.isNullOrBlank() && result.summary != "null") mediaData["summary"] = result.summary else mediaData["summary"] = null
                    if (!result.staff.isNullOrBlank() && result.staff != "null") mediaData["staff"] = result.staff else mediaData["staff"] = null
                    if (result.ratingDouban in 0.0..10.0) mediaData["rating_douban"] = result.ratingDouban else mediaData["rating_douban"] = null
                    if (result.ratingImdb in 0.0..10.0) mediaData["rating_imdb"] = result.ratingImdb else mediaData["rating_imdb"] = null
                    if (result.ratingBangumi in 0.0..10.0) mediaData["rating_bangumi"] = result.ratingBangumi else mediaData["rating_bangumi"] = null
                    val doc = databases.createDocument(
                        dbId, mediaColId, ID.unique(), mediaData
                    )
                    mediaId = doc.id
                }

                // 2. 检查media_source是否存在
                val sourceQuery = listOf(Query.equal("source_id", result.sourceId))
                val sourceDocs = databases.listDocuments(
                    dbId, mediaSourceColId, sourceQuery
                ).documents

                if (sourceDocs.isEmpty()) {
                    // 如果media_source不存在，创建新记录
                    val sourceData = mapOf(
                        "media_id" to mediaId,
                        "source_type" to result.sourceType,
                        "source_id" to result.sourceId,
                        "source_url" to result.sourceUrl
                    )
                    databases.createDocument(dbId, mediaSourceColId, ID.unique(), sourceData)
                }

                // 3. 检查collection是否存在该user_id+media_id组合
                val collectionQuery = listOf(
                    Query.equal("user_id", userId),
                    Query.equal("media_id", mediaId)
                )
                val collectionDocs = databases.listDocuments(
                    dbId, collectionColId, collectionQuery
                ).documents

                if (collectionDocs.isEmpty()) {
                    // 若collection中不存在此组合，则插入
                    val now = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }.format(Date())
                    val collectionData = mapOf(
                        "user_id" to userId,
                        "media_id" to mediaId,
                        "added_time" to now,
                        "watch_status" to false,
                        "notes" to ""
                    )
                    databases.createDocument(dbId, collectionColId, ID.unique(), collectionData)
                }

                // 操作完成，无论是跳过还是创建新记录，都视为成功
                callback(true)
            } catch (e: Exception) {
                android.util.Log.e("AppwriteDebug", "addMediaWithSourceAndCollection error: ${e.message}", e)
                callback(false)
            }
        }
    }

    /**
     * 获取用户收藏记录
     * @param userId 用户ID
     * @param onSuccess 成功回调，返回收藏记录列表
     * @param onError 失败回调
     */
    fun getUserCollections(userId: String, onSuccess: (List<Map<String, Any>>) -> Unit, onError: (Exception) -> Unit) {
        appwriteScope.launch {
            try {
                checkInitialized()

                val documents = databases.listDocuments(
                    AppConfig.DATABASE_ID,
                    AppConfig.COLLECTION_COLLECTIONS_ID,
                    queries = listOf(Query.equal("user_id", userId))
                )

                withContext(Dispatchers.Main) {
                    onSuccess(documents.documents.map { it.data })
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onError(e)
                }
            }
        }
    }

    /**
     * 通过ID获取媒体信息
     * @param mediaId 媒体ID
     * @param onSuccess 成功回调，返回媒体信息
     * @param onError 失败回调
     */
    fun getMediaById(mediaId: String, onSuccess: (Map<String, Any>) -> Unit, onError: (Exception) -> Unit) {
        appwriteScope.launch {
            try {
                checkInitialized()

                val documents = databases.listDocuments(
                    AppConfig.DATABASE_ID,
                    AppConfig.COLLECTION_MEDIA_ID,
                    queries = listOf(Query.equal("\$id", mediaId))
                )

                if (documents.documents.isEmpty()) {
                    withContext(Dispatchers.Main) {
                        onError(Exception("媒体信息不存在"))
                    }
                    return@launch
                }

                withContext(Dispatchers.Main) {
                    onSuccess(documents.documents[0].data)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onError(e)
                }
            }
        }
    }

    /**
     * 从收藏中移除（根据sourceId查询并删除）
     * @param sourceId 来源ID
     * @param callback 回调函数，参数为是否成功
     */
    fun removeFromCollection(sourceId: String, callback: (Boolean) -> Unit) {
        appwriteScope.launch {
            try {
                checkInitialized()
                val dbId = AppConfig.DATABASE_ID
                val mediaSourceColId = AppConfig.COLLECTION_MEDIA_SOURCE_ID
                val collectionColId = AppConfig.COLLECTION_COLLECTIONS_ID

                try {
                    // 1. 通过sourceId查询media_source表获取media_id
                    val mediaSourceQuery = listOf(Query.equal("source_id", sourceId))
                    val mediaSourceDocs = databases.listDocuments(
                        dbId,
                        mediaSourceColId,
                        mediaSourceQuery
                    )

                    if (mediaSourceDocs.documents.isEmpty()) {
                        withContext(Dispatchers.Main) {
                            callback(false)
                        }
                        return@launch
                    }

                    val mediaId = mediaSourceDocs.documents[0].data["media_id"] as String

                    // 2. 获取当前用户ID
                    val currentUser = account.get()
                    val userId = currentUser.id

                    // 3. 根据userId和mediaId查询collection表
                    val collectionQuery = listOf(
                        Query.equal("user_id", userId),
                        Query.equal("media_id", mediaId)
                    )
                    val collectionDocs = databases.listDocuments(
                        dbId,
                        collectionColId,
                        collectionQuery
                    )

                    if (collectionDocs.documents.isEmpty()) {
                        withContext(Dispatchers.Main) {
                            callback(false)
                        }
                        return@launch
                    }

                    // 4. 删除collection记录
                    val collectionDocId = collectionDocs.documents[0].id
                    databases.deleteDocument(dbId, collectionColId, collectionDocId)

                    withContext(Dispatchers.Main) {
                        callback(true)
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        callback(false)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    callback(false)
                }
            }
        }
    }


    /**
     * 更新观看状态
     * @param userId 用户ID
     * @param mediaId 媒体ID
     * @param watchStatus 观看状态
     * @param callback 回调函数
     */
    fun updateWatchStatus(userId: String, mediaId: String, watchStatus: Boolean, callback: (Boolean) -> Unit) {
        appwriteScope.launch {
            try {
                checkInitialized()
                val dbId = AppConfig.DATABASE_ID
                val collectionColId = AppConfig.COLLECTION_COLLECTIONS_ID

                try {
                    // 1. 根据userId和mediaId查询collection表
                    val collectionQuery = listOf(
                        Query.equal("user_id", userId),
                        Query.equal("media_id", mediaId)
                    )
                    val collectionDocs = databases.listDocuments(
                        dbId,
                        collectionColId,
                        collectionQuery
                    )

                    if (collectionDocs.documents.isEmpty()) {
                        withContext(Dispatchers.Main) {
                            callback(false)
                        }
                        return@launch
                    }

                    // 2. 更新collection记录
                    val collectionDoc = collectionDocs.documents[0]
                    val collectionId = collectionDoc.id

                    // 3. 准备更新数据
                    val updateData = mapOf(
                        "watch_status" to watchStatus
                    )

                    // 4. 执行更新
                    databases.updateDocument(
                        dbId,
                        collectionColId,
                        collectionId,
                        updateData
                    )

                    withContext(Dispatchers.Main) {
                        callback(true)
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        callback(false)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    callback(false)
                }
            }
        }
    }

    /**
     * 查询文档列表（Java调用版本）
     * @param databaseId 数据库ID
     * @param collectionId 集合ID
     * @param queriesArray 查询条件数组，格式为[字段,操作符,值,字段,操作符,值...]
     * @param onSuccess 成功回调
     * @param onError 失败回调
     */
    fun listDocumentsForJava(
        databaseId: String,
        collectionId: String,
        queriesArray: Array<String>,
        onSuccess: (List<Map<String, Any>>) -> Unit,
        onError: (Exception) -> Unit
    ) {
        appwriteScope.launch {
            try {
                checkInitialized()

                // 使用字符串构建查询条件
                val queryStrings = mutableListOf<String>()
                if (queriesArray.isNotEmpty() && queriesArray.size >= 3) {
                    var i = 0
                    while (i <= queriesArray.size - 3) {
                        val field = queriesArray[i]
                        val operator = queriesArray[i + 1]
                        val value = queriesArray[i + 2]

                        try {
                            // 将每个查询条件构建为字符串
                            when (operator) {
                                "equal" -> {
                                    val query = Query.equal(field, value)
                                    queryStrings.add(query.toString())
                                }
                                "notEqual" -> {
                                    val query = Query.notEqual(field, value)
                                    queryStrings.add(query.toString())
                                }
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("Appwrite", "创建查询条件失败: ${e.message}", e)
                        }

                        i += 3
                    }
                }

                try {
                    // 使用字符串列表进行查询
                    val documents = databases.listDocuments(
                        databaseId,
                        collectionId,
                        queryStrings
                    )

                    withContext(Dispatchers.Main) {
                        onSuccess(documents.documents.map { it.data })
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        onError(e)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onError(e)
                }
            }
        }
    }
}