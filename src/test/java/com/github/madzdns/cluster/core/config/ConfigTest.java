package com.github.madzdns.cluster.core.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ConfigTest {

    @Test
    void initTest() {
        App app = Config.getApp();
        assertEquals(1,app.getId());
    }
}