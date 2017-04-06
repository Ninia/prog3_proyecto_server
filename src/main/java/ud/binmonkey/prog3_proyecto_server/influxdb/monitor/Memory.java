package ud.binmonkey.prog3_proyecto_server.influxdb.monitor;

import ud.binmonkey.prog3_proyecto_server.common.posix.ShellCommand;

public class Memory {

    public static long getUsage() {
        return Long.parseLong(
                ShellCommand.executeCommand("/usr/local/p3p/mem-usage.sh").replace("\n", "")
        );
    }

    public static void main(String[] args) {
        while (true) {
            System.out.println("Memory usage: " +
                    "" + Memory.getUsage());
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
