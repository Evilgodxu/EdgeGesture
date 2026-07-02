// ICommandService.aidl
package com.edgegesture.evilgodxu.service;

// 命令执行服务接口，用于 Shizuku UserService
interface ICommandService {
    // 执行 shell 命令，返回执行结果
    String executeCommand(String command);

    // 强制停止指定包名的应用
    boolean forceStopPackage(String packageName);

    // 检查服务是否存活
    boolean isAlive();
}
