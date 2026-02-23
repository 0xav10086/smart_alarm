package io.github.av10086.smartalarm

import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface.ModuleLoadedParam
import io.github.libxposed.api.XposedModuleInterface.PackageLoadedParam
import io.github.libxposed.api.annotations.BeforeInvocation
import io.github.libxposed.api.annotations.XposedHooker
import java.io.File
import kotlin.concurrent.thread

private lateinit var module: ModuleMain

class ModuleMain(base: XposedInterface, param: ModuleLoadedParam) : XposedModule(base, param) {

    init {
        module = this
    }

    // 定义一个 Hooker 类，用于拦截 Gadgetbridge 的方法

    @XposedHooker
    class SyncHooker : XposedInterface.Hooker {
        companion object {
            @JvmStatic
            @BeforeInvocation
            fun beforeInvocation(callback: XposedInterface.BeforeHookCallback) {
                val instance = callback.thisObject ?: return

                // 使用字符串拼接绕过硬编码检查，或直接忽略警告
                // 这里的路径必须是 GB 的私有目录，因为 Shell 脚本 touch 的就是这里
                val triggerPath = "/data/data/nodomain.freeyourgadget.gadgetbridge/cache/smart_alarm_trigger"
                val triggerFile = File(triggerPath)

                if (triggerFile.exists()) {
                    module.log("检测到同步信号文件")
                    try {
                        // 反射调用 GBDeviceAdapterv2.onFetchRecordedData(1)
                        val method = instance.javaClass.getMethod("onFetchRecordedData", Int::class.javaPrimitiveType ?: Int::class.java)
                        method.invoke(instance, 1)

                        module.log("同步指令已通过反射发送")
                        triggerFile.delete()
                    } catch (e: Exception) {
                        module.log("反射调用失败: ${e.stackTraceToString()}")
                    }
                }
            }
        }
    }

    override fun onPackageLoaded(param: PackageLoadedParam) {
        super.onPackageLoaded(param)

        // 1. 确认我们进入了 Gadgetbridge 进程
        if (param.packageName != "nodomain.freeyourgadget.gadgetbridge") return

        log("成功注入 Gadgetbridge 进程，准备挂载 Hook...")

        runCatching {
            // 2. 找到你在脚本注释中提到的目标类：GBDeviceAdapterv2
            val adapterClass = param.classLoader.loadClass("nodomain.freeyourgadget.gadgetbridge.adapter.GBDeviceAdapterv2")

            // 3. 我们 Hook 这个类的任一高频调用的方法，作为检查信号的切入点
            // 这里我们选择 Hook 它本身，也就是 onFetchRecordedData 方法
            // 这样每次系统尝试获取数据时，我们都会顺便检查一下我们的自定义信号
            val targetMethod = adapterClass.getDeclaredMethod("onFetchRecordedData", Int::class.java)

            hook(targetMethod, SyncHooker::class.java)
            log("Hook GBDeviceAdapterv2 成功，实时同步监控已就绪")

        }.onFailure {
            log("Hook 失败，请检查 Gadgetbridge 版本是否变更: ${it.message}")
        }
    }
}
