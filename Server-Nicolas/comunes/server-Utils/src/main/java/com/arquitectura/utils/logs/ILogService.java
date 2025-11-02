package com.arquitectura.utils.logs;

import java.io.IOException;

public interface ILogService {
    String getLogContents() throws IOException;
}
