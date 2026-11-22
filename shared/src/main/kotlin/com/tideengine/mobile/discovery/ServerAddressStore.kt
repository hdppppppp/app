package com.tideengine.mobile.discovery

/**
 * 探测结果的本地持久化契约：记住上次成功连接的服务端地址。
 *
 * 接口定义在 shared、实现由宿主（androidApp）注入 SharedPreferences 版本，
 * 与 TokenStorage 的分层方式一致 —— shared 不触碰 Android API。
 * 读取发生在启动探测的第一步，实现必须同步可查。
 */
interface ServerAddressStore {
    /** 上次成功连接的地址；从未保存或被清除时返回 null。 */
    fun load(): String?

    /** 保存当前生效的地址；调用时机是探测 / 手动输入验证通过之后。 */
    fun save(baseUrl: String)
}
