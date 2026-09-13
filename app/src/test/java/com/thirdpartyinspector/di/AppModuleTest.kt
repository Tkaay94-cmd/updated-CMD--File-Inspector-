package com.thirdpartyinspector.di

import com.thirdpartyinspector.scanner.RiskEngine
import org.junit.Assert.assertTrue
import org.junit.Test

class AppModuleTest {
    @Test
    fun `provideRiskEngine returns a RiskEngine instance`() {
        val riskEngine = AppModule.provideRiskEngine()

        assertTrue(riskEngine is RiskEngine)
    }
}
