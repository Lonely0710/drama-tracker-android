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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

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
            Log.e("Appwrite", "Appwrite服务尚未初始化，请先调用Appwrite.init(context)")
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
            Log.d("Appwrite", "开始初始化Appwrite服务")
            client = Client(context)
                .setEndpoint("https://cloud.appwrite.io/v1")
                .setProject(AppConfig.PROJECT_ID)
                .setSelfSigned(true) // 允许自签名证书

            account = Account(client)
            databases = Databases(client)
            storage = Storage(client)
            
            isInitialized = true
            Log.d("Appwrite", "Appwrite服务初始化成功")
        } catch (e: Exception) {
            Log.e("Appwrite", "Appwrite服务初始化失败: ${e.message}", e)
            throw e
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
                Log.d("Appwrite", "登录成功，用户ID: ${session.userId}")

                withContext(Dispatchers.Main) {
                    onSuccess(session)
                }
            } catch (e: Exception) {
                Log.e("Appwrite", "登录失败: ${e.message}", e)
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
                Log.d("Appwrite", "生成用户ID: $userId")

                // 先尝试删除现有会话
                try {
                    account.deleteSession("current")
                    Log.d("Appwrite", "已删除现有会话")
                } catch (e: Exception) {
                    // 忽略删除会话时的错误，可能本来就没有会话
                    Log.d("Appwrite", "没有现有会话需要删除或删除失败")
                }

                val user = account.create(
                    userId,
                    email,
                    password,
                    name
                )

                // 2. 创建用户文档
                val avatarUrl = "https://ui-avatars.com/api/?name=${name.replace(" ", "+")}"
                val documentId = createUser(email, name, userId, avatarUrl)
                Log.d("Appwrite", "用户文档ID: $documentId")

                // 3. 登录
                try {
                    val session = account.createEmailPasswordSession(email, password)
                    Log.d("Appwrite", "自动登录成功: ${session.userId}")
                } catch (e: Exception) {
                    Log.e("Appwrite", "自动登录失败: ${e.message}", e)
                }

                // 返回结果
                withContext(Dispatchers.Main) {
                    onSuccess(user)
                }
            } catch (e: Exception) {
                Log.e("Appwrite", "注册失败: ${e.message}", e)
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
                
                Log.d("Appwrite", "开始获取当前用户信息")
                val userData = getCurrentUser()
                if (userData != null) {
                    Log.d("Appwrite", "成功获取用户信息，准备回调")
                    Log.d("Appwrite", "完整用户数据: $userData")
                    if (userData.containsKey("avatarUrl")) {
                        Log.d("Appwrite", "头像URL: ${userData["avatarUrl"]}")
                    } else {
                        Log.d("Appwrite", "用户数据中不包含avatarUrl字段")
                    }
                    withContext(Dispatchers.Main) {
                        onSuccess(userData)
                    }
                } else {
                    Log.e("Appwrite", "获取用户信息返回null")
                    withContext(Dispatchers.Main) {
                        onError(Exception("用户信息为空"))
                    }
                }
            } catch (e: Exception) {
                Log.e("Appwrite", "获取用户信息时发生异常: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    onError(e)
                }
            }
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
                
                Log.d("Appwrite", "尝试创建用户文档 (尝试 ${retryCount + 1}/$MAX_RETRIES)")
                Log.d("Appwrite", "数据库ID: ${AppConfig.DATABASE_ID}")
                Log.d("Appwrite", "集合ID: ${AppConfig.COLLECTION_USERS_ID}")
                
                val userData = mapOf(
                    "user_id" to userId,
                    "email" to email,
                    "name" to name,
                    "avatar_url" to avatarUrl
                )
                
                Log.d("Appwrite", "创建用户文档 - 传入数据详情: user_id=$userId, email=$email, name=$name, avatar_url=$avatarUrl")
                
                val document = databases.createDocument(
                    AppConfig.DATABASE_ID,
                    AppConfig.COLLECTION_USERS_ID,
                    ID.unique(),  // 使用唯一ID作为文档ID，而不是userId
                    userData,
                )
                
                Log.d("Appwrite", "用户文档创建成功: 完整数据=${document.data}")
                
                return document.id
            } catch (e: Exception) {
                lastException = e
                Log.e("Appwrite", "创建用户文档失败 (尝试 ${retryCount + 1}/$MAX_RETRIES): ${e.message}", e)
                if (retryCount < MAX_RETRIES - 1) {
                    Log.d("Appwrite", "等待 ${RETRY_DELAY}ms 后重试...")
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
                
                // 1. 创建用户账号
                Log.d("Appwrite", "注册开始 - Email: $email, Name: $name")
                
                val user = account.create(
                    ID.unique(), // 使用随机ID
                    email,
                    password,
                    name
                )
                val userId = user.id
                
                Log.d("Appwrite", "用户账号创建成功: ID=$userId")
                
                // 生成头像URL
                val avatarUrl = "https://ui-avatars.com/api/?name=${name.replace(" ", "+")}"
                
                // 2. 创建用户文档
                val documentId = createUser(email, name, userId, avatarUrl)
                Log.d("Appwrite", "用户文档ID: $documentId")
                
                // 3. 登录
                try {
                    val session = account.createEmailPasswordSession(email, password)
                    Log.d("Appwrite", "自动登录成功: ${session.userId}")
                } catch (e: Exception) {
                    Log.e("Appwrite", "自动登录失败: ${e.message}", e)
                    // 登录失败不影响注册结果
                }
                
                userId // 返回用户ID
            } catch (e: Exception) {
                Log.e("Appwrite", "注册失败: ${e.message}", e)
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
                Log.d("Appwrite", "登录成功: ${session.userId}")
                session
            } catch (e: Exception) {
                Log.e("Appwrite", "登录失败: ${e.message}", e)
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
                Log.d("Appwrite", "获取到当前用户: ${currentUser.name}, ID: ${currentUser.id}")
                
                // 确保用户存在
                if (currentUser.id != null) {
                    // 查询用户文档以获取更多信息
                    val response = databases.listDocuments(
                        AppConfig.DATABASE_ID,
                        AppConfig.COLLECTION_USERS_ID,
                        listOf(
                            io.appwrite.Query.equal("user_id", currentUser.id)
                        )
                    )
                    
                    Log.d("Appwrite", "查询到用户文档数: ${response.documents.size}")
                    
                    if (response.documents.isNotEmpty()) {
                        val userDoc = response.documents[0]
                        Log.d("Appwrite", "用户文档原始数据: ${userDoc.data}")
                        
                        val userData = userDoc.data.toMutableMap()
                        
                        // 添加从账户中获取的其他字段
                        userData["name"] = currentUser.name
                        userData["email"] = currentUser.email
                        
                        // 确保头像URL正确，检查原始字段存在
                        val avatarUrl = userData["avatar_url"] as? String
                        Log.d("Appwrite", "原始头像URL: $avatarUrl")

                        // 如果原始URL为空，生成一个更安全的头像URL
                        if (avatarUrl.isNullOrEmpty()) {
                            val encodedName = URLEncoder.encode(currentUser.name, "UTF-8")
                            // 使用更多参数，指定格式为.png
                            val defaultAvatarUrl = "https://ui-avatars.com/api/?name=$encodedName&size=200&background=random&format=png&rounded=true"
                            userData["avatarUrl"] = defaultAvatarUrl
                            Log.d("Appwrite", "生成新的头像URL: $defaultAvatarUrl")
                        } else {
                            userData["avatarUrl"] = avatarUrl
                        }
                        
                        Log.d("Appwrite", "最终处理后的数据: name=${userData["name"]}, avatarUrl=${userData["avatarUrl"]}")
                        return@withContext userData
                    } else {
                        // 找不到用户文档时，生成默认头像并返回基本信息
                        val defaultAvatarUrl = "https://ui-avatars.com/api/?name=${currentUser.name.replace(" ", "+")}"
                        val basicData = mapOf(
                            "name" to currentUser.name,
                            "email" to currentUser.email,
                            "avatarUrl" to defaultAvatarUrl
                        )
                        Log.d("Appwrite", "未找到用户文档，使用默认头像: $defaultAvatarUrl")
                        return@withContext basicData
                    }
                }
                
                // 没有当前用户时返回 null
                Log.d("Appwrite", "没有当前登录用户")
                null
            } catch (e: Exception) {
                Log.e("Appwrite", "获取当前用户失败: ${e.message}", e)
                null
            }
        }
    }

    // 登出 - 回调版本
    fun logoutWithCallback(onSuccess: () -> Unit, onError: (Exception) -> Unit) {
        appwriteScope.launch {
            try {
                checkInitialized()
                
                Log.d("Appwrite", "开始登出")
                account.deleteSession("current")
                Log.d("Appwrite", "登出成功")
                
                withContext(Dispatchers.Main) {
                    onSuccess()
                }
            } catch (e: Exception) {
                Log.e("Appwrite", "登出失败: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    onError(e)
                }
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
                Log.e("Appwrite", "登出失败: ${e.message}", e)
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
                Log.e("Appwrite", "获取当前用户ID失败: ${e.message}", e)
            }
        }
        return userId
    }

    // Java可调用的三表插入方法
    fun addMediaWithSourceAndCollection(result: SearchResult, userId: String, callback: (Boolean) -> Unit) {
        android.util.Log.d("AppwriteDebug", "addMediaWithSourceAndCollection called, result=$result, userId=$userId")
        appwriteScope.launch {
            try {
                checkInitialized()
                val dbId = AppConfig.DATABASE_ID
                val mediaColId = AppConfig.COLLECTION_MEDIA_ID
                val mediaSourceColId = AppConfig.COLLECTION_MEDIA_SOURCE_ID
                val collectionColId = AppConfig.COLLECTION_COLLECTIONS_ID
                android.util.Log.d("AppwriteDebug", "Appwrite config: dbId=$dbId, mediaColId=$mediaColId, mediaSourceColId=$mediaSourceColId, collectionColId=$collectionColId")
                
                // 1. 检查media是否存在
                var mediaId: String
                val mediaQuery = listOf(
                    Query.equal("title_zh", result.titleZh),
                    Query.equal("release_date", result.releaseDate)
                )
                android.util.Log.d("AppwriteDebug", "Querying media: title_zh=${result.titleZh}, release_date=${result.releaseDate}")
                val mediaList = databases.listDocuments(
                    dbId, mediaColId, mediaQuery
                ).documents
                
                if (mediaList.isNotEmpty()) {
                    // 如果media已存在，使用现有ID
                    android.util.Log.d("AppwriteDebug", "media exists, id=${mediaList[0].id}")
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
                    android.util.Log.d("AppwriteDebug", "Inserting new media: $mediaData")
                    val doc = databases.createDocument(
                        dbId, mediaColId, ID.unique(), mediaData
                    )
                    android.util.Log.d("AppwriteDebug", "Inserted media id=${doc.id}")
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
                    android.util.Log.d("AppwriteDebug", "Inserting media_source: $sourceData")
                    databases.createDocument(dbId, mediaSourceColId, ID.unique(), sourceData)
                } else {
                    android.util.Log.d("AppwriteDebug", "media_source exists, skipping insertion")
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
                    android.util.Log.d("AppwriteDebug", "Inserting collection: $collectionData")
                    databases.createDocument(dbId, collectionColId, ID.unique(), collectionData)
                    android.util.Log.d("AppwriteDebug", "addMediaWithSourceAndCollection success, inserted to collection table")
                } else {
                    android.util.Log.d("AppwriteDebug", "collection record already exists for user_id=$userId and media_id=$mediaId, skipping insertion")
                }
                
                // 操作完成，无论是跳过还是创建新记录，都视为成功
                callback(true)
            } catch (e: Exception) {
                android.util.Log.e("AppwriteDebug", "addMediaWithSourceAndCollection error: ${e.message}", e)
                callback(false)
            }
        }
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
                Log.e("Appwrite", "获取用户收藏记录失败: ${e.message}", e)
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
                
                Log.d("Appwrite", "开始获取媒体信息，mediaId=$mediaId")
                val documents = databases.listDocuments(
                    AppConfig.DATABASE_ID,
                    AppConfig.COLLECTION_MEDIA_ID,
                    queries = listOf(Query.equal("\$id", mediaId))
                )
                
                if (documents.documents.isEmpty()) {
                    Log.e("Appwrite", "未找到媒体信息: $mediaId")
                    withContext(Dispatchers.Main) {
                        onError(Exception("媒体信息不存在"))
                    }
                    return@launch
                }
                
                Log.d("Appwrite", "成功获取媒体信息: ${documents.documents[0].data}")
                withContext(Dispatchers.Main) {
                    onSuccess(documents.documents[0].data)
                }
            } catch (e: Exception) {
                Log.e("Appwrite", "获取媒体信息失败: ${e.message}", e)
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
                
                Log.d("Appwrite", "开始移除收藏，sourceId=$sourceId")
                
                try {
                    // 1. 通过sourceId查询media_source表获取media_id
                    Log.d("Appwrite", "查询media_source表，sourceId=$sourceId")
                    val mediaSourceQuery = listOf(Query.equal("source_id", sourceId))
                    val mediaSourceDocs = databases.listDocuments(
                        dbId, 
                        mediaSourceColId,
                        mediaSourceQuery
                    )
                    
                    if (mediaSourceDocs.documents.isEmpty()) {
                        Log.e("Appwrite", "未找到对应的media_source记录")
                        withContext(Dispatchers.Main) {
                            callback(false)
                        }
                        return@launch
                    }
                    
                    val mediaId = mediaSourceDocs.documents[0].data["media_id"] as String
                    Log.d("Appwrite", "获取到media_id=$mediaId")
                    
                    // 2. 获取当前用户ID
                    val currentUser = account.get()
                    val userId = currentUser.id
                    Log.d("Appwrite", "当前用户ID=$userId")
                    
                    // 3. 根据userId和mediaId查询collection表
                    Log.d("Appwrite", "查询collection表，userId=$userId, mediaId=$mediaId")
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
                        Log.e("Appwrite", "未找到对应的collection记录")
                        withContext(Dispatchers.Main) {
                            callback(false)
                        }
                        return@launch
                    }
                    
                    // 4. 删除collection记录
                    val collectionDocId = collectionDocs.documents[0].id
                    Log.d("Appwrite", "开始删除collection记录，id=$collectionDocId")
                    databases.deleteDocument(dbId, collectionColId, collectionDocId)
                    
                    Log.d("Appwrite", "成功删除收藏记录")
                    withContext(Dispatchers.Main) {
                        callback(true)
                    }
                } catch (e: Exception) {
                    Log.e("Appwrite", "删除收藏记录失败: ${e.message}", e)
                    withContext(Dispatchers.Main) {
                        callback(false)
                    }
                }
            } catch (e: Exception) {
                Log.e("Appwrite", "移除收藏失败: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    callback(false)
                }
            }
        }
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
                
                Log.d("Appwrite", "查询文档，database=$databaseId, collection=$collectionId, queries=$queries")
                
                // 转换Query对象为字符串列表
                val queryStrings = queries.map { it.toString() }
                
                val documents = databases.listDocuments(
                    databaseId,
                    collectionId,
                    queries = queryStrings
                )
                
                Log.d("Appwrite", "查询到 ${documents.documents.size} 条记录")
                withContext(Dispatchers.Main) {
                    onSuccess(documents.documents.map { it.data })
                }
            } catch (e: Exception) {
                Log.e("Appwrite", "查询文档失败: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    onError(e)
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
                
                Log.d("Appwrite", "从Java调用查询文档，database=$databaseId, collection=$collectionId")
                
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
                            Log.d("Appwrite", "添加查询条件: $operator($field, $value)")
                        } catch (e: Exception) {
                            Log.e("Appwrite", "创建查询条件失败: ${e.message}", e)
                        }
                        
                        i += 3
                    }
                }
                
                Log.d("Appwrite", "查询条件数量: ${queryStrings.size}")
                
                try {
                    // 使用字符串列表进行查询
                    val documents = databases.listDocuments(
                        databaseId,
                        collectionId,
                        queryStrings
                    )
                    
                    Log.d("Appwrite", "查询成功，结果数量: ${documents.documents.size}")
                    withContext(Dispatchers.Main) {
                        onSuccess(documents.documents.map { it.data })
                    }
                } catch (e: Exception) {
                    Log.e("Appwrite", "查询失败: ${e.message}", e)
                    withContext(Dispatchers.Main) {
                        onError(e)
                    }
                }
            } catch (e: Exception) {
                Log.e("Appwrite", "查询文档失败: ${e.message}", e)
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
} 