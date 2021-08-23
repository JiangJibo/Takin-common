package io.shulie.takin.channel.impl;

import io.shulie.takin.channel.CommandRegistry;
import io.shulie.takin.channel.handler.CommandHandler;
import io.shulie.takin.channel.type.Command;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author: Hengyu
 * @className: DefaultCommandRegistry
 * @date: 2020/12/29 11:42 下午
 * @description:
 */
public class DefaultCommandRegistry implements CommandRegistry {

    private ConcurrentHashMap<String,CommandHandler> registry;

    public DefaultCommandRegistry() {
        this.registry = new ConcurrentHashMap<>();
    }

    @Override
    public void register(Command command, CommandHandler handler) {
        this.registry.put(command.getId(),handler);
    }

    @Override
    public CommandHandler getHandler(String commandId) {
        return this.registry.get(commandId);
    }

}
