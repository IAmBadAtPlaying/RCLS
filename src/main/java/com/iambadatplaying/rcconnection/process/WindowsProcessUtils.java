package com.iambadatplaying.rcconnection.process;

import java.nio.file.Path;
import java.util.ArrayList;

public final class WindowsProcessUtils {
    private WindowsProcessUtils() {}

    public static String[] buildWMICommand(String executableName, String... fields) {

        StringBuilder query = new StringBuilder("SELECT ");
        for (String field : fields) {
            query.append(field).append(",");
        }
        query.deleteCharAt(query.length() - 1)
                .append(" FROM Win32_Process WHERE Name='")
                .append(executableName)
                .append("'");

        return new String[]{
                "powershell.exe",
                "-Command",
                "(Get-WmiObject -Query \\\"" + query +"\\\")",
                "|",
                "ConvertTo-Json"
        };
    }

    public static String[] buildTaskkillCommand(int processId) {
        return new String[]{
                "taskkill",
                "/F",
                "/PID",
                String.valueOf(processId)
        };
    }

    public static String[] buildStartRCSWithAuthTokenCommand(
            Path riotClientServicePath,
            int port,
            String authToken,
            boolean headless
    ) {
        ArrayList<String> command = new ArrayList<>();
        command.add("\""+riotClientServicePath.toString()+"\"");
        if (headless) {
            command.add("--headless");
        }
        command.add("--remoting-auth-token="+authToken);
        command.add("--app-port=" + port );
        return command.toArray(new String[0]);
    }

    public static String[] buildStartRiotClientCommand(
            Path riotClientPath,
            int port,
            int rcsPid,
            String authToken
    ) {
        return new String[]{
                "cmd",
                "/c",
                "\""+riotClientPath.toString()+"\"",
                "--app-port=" + port,
                "--app-pid=" + rcsPid,
                "--remoting-auth-token=" + authToken
        };
    }
}
