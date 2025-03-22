package commands;

import application.Application;
import results.Result;
import java.io.Serializable;

// Command.java (interface)
public interface Command extends Serializable {
    Result execute(Application app);

}