package io.github.av10086.smartalarm

import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface

// 手动配置 LibXposed 时，注解可能无法通过编译，我们可以暂时注释掉它们
// 或者确保 import 路径正确。由于我们已经手动创建了 libxposed_init，注解在运行时并非必需。
class ModuleMain(
    iface: XposedInterface,
    param: XposedModuleInterface.ModuleLoadedParam
) : XposedModule(iface, param) {

    override fun onPackageLoaded(param: XposedModuleInterface.PackageLoadedParam) {
        super.onPackageLoaded(param)

        // 只有进入 Gadgetbridge 进程时才执行
        if (param.packageName == "nodomain.freeyourgadget.gadgetbridge") {
            log("Smart Alarm: 成功注入 Gadgetbridge!")

            // 示例：调用查询逻辑
            // val data = GetDataInDB.queryLatestData()
            // log("Hook 进程读取结果: $data")
        }
    }
}
