package com.lonely.dramatracker.config

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.util.Properties

/**
 * 应用配置类
 * 负责读取和管理应用配置信息
 */
object AppConfig {
    private const val TAG = "AppConfig"
    private const val SECRETS_FILE = "secrets.properties"
    
    // AppWrite配置
    lateinit var PROJECT_ID: String
    lateinit var DATABASE_ID: String
    
    // 集合ID配置
    lateinit var COLLECTION_USERS_ID: String
    lateinit var COLLECTION_COLLECTIONS_ID: String
    lateinit var COLLECTION_MEDIA_ID: String
    lateinit var COLLECTION_MEDIA_SOURCE_ID: String
    
    // TMDB API配置
    lateinit var TMDB_API_TOKEN: String
    lateinit var TMDB_API_KEY: String
    
    // 为Java代码提供静态引用
    @JvmField
    var DATABASE_ID_STATIC: String = ""
    
    @JvmField
    var COLLECTION_USERS_ID_STATIC: String = ""
    
    @JvmField
    var COLLECTION_COLLECTIONS_ID_STATIC: String = ""
    
    @JvmField
    var COLLECTION_MEDIA_ID_STATIC: String = ""
    
    @JvmField
    var COLLECTION_MEDIA_SOURCE_ID_STATIC: String = ""
    
    @JvmField
    var TMDB_API_TOKEN_STATIC: String = ""
    
    @JvmField
    var TMDB_API_KEY_STATIC: String = ""
    
    /**
     * 初始化配置
     * @param context 应用上下文
     */
    fun init(context: Context) {
        try {
            // 从assets目录读取配置文件
            val inputStream: InputStream = context.assets.open(SECRETS_FILE)
            
            // 读取配置文件
            val properties = Properties()
            inputStream.use { 
                properties.load(it)
            }
            
            // 读取AppWrite配置
            PROJECT_ID = properties.getProperty("APPWRITE_PROJECT_ID")
                ?: throw RuntimeException("APPWRITE_PROJECT_ID not found in config")
            DATABASE_ID = properties.getProperty("APPWRITE_DATABASE_ID")
                ?: throw RuntimeException("APPWRITE_DATABASE_ID not found in config")
            
            // 读取集合ID配置
            COLLECTION_USERS_ID = properties.getProperty("APPWRITE_COLLECTION_USERS_ID")
                ?: throw RuntimeException("APPWRITE_COLLECTION_USERS_ID not found in config")
            COLLECTION_COLLECTIONS_ID = properties.getProperty("APPWRITE_COLLECTION_COLLECTIONS_ID")
                ?: throw RuntimeException("APPWRITE_COLLECTION_COLLECTIONS_ID not found in config")
            COLLECTION_MEDIA_ID = properties.getProperty("APPWRITE_COLLECTION_MEDIA_ID")
                ?: throw RuntimeException("APPWRITE_COLLECTION_MEDIA_ID not found in config")
            COLLECTION_MEDIA_SOURCE_ID = properties.getProperty("APPWRITE_COLLECTION_MEDIA_SOURCE_ID")
                ?: throw RuntimeException("APPWRITE_COLLECTION_MEDIA_SOURCE_ID not found in config")
            
            // 读取TMDB API配置
            TMDB_API_TOKEN = properties.getProperty("TMDB_API_TOKEN")
                ?: throw RuntimeException("TMDB_API_TOKEN not found in config")
            TMDB_API_KEY = properties.getProperty("TMDB_API_KEY")
                ?: throw RuntimeException("TMDB_API_KEY not found in config")
            
            // 为Java静态字段赋值
            DATABASE_ID_STATIC = DATABASE_ID
            COLLECTION_USERS_ID_STATIC = COLLECTION_USERS_ID
            COLLECTION_COLLECTIONS_ID_STATIC = COLLECTION_COLLECTIONS_ID
            COLLECTION_MEDIA_ID_STATIC = COLLECTION_MEDIA_ID
            COLLECTION_MEDIA_SOURCE_ID_STATIC = COLLECTION_MEDIA_SOURCE_ID
            TMDB_API_TOKEN_STATIC = TMDB_API_TOKEN
            TMDB_API_KEY_STATIC = TMDB_API_KEY
            
            Log.d(TAG, "配置加载成功")
        } catch (e: Exception) {
            Log.e(TAG, "加载配置文件失败: ${e.message}")
            throw RuntimeException("无法加载应用配置，请确保secrets.properties文件存在且包含所有必需的配置项", e)
        }
    }
} 