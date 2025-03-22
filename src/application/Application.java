package application;

import results.Result;
import commands.Command;
public interface Application {
    Result execute(Command cmd);
}
