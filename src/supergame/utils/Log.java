
package supergame.utils;

import java.util.logging.Level;
import java.util.logging.Logger;

public class Log {
    public static Logger getLogger(String name, Level level) {
        Logger logger = Logger.getLogger(name);
        logger.setLevel(level);
        return logger;
    }
}
