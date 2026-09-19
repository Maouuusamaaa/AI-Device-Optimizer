package com.maouuusama.ai.device.optimizer.monitor;

interface IShizukuShellService {
    void destroy() = 16777114;
    String execute(String operation, long timeoutMs) = 2;
}
