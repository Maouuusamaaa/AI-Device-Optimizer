package com.maouuusama.ai.device.optimizer.monitor;

interface IShizukuShellService {
    void destroy() = 16777114;
    void exit() = 1;
    String execute(String command, long timeoutMs) = 2;
}
