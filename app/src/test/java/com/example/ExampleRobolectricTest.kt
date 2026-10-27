package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.parser.WordPressDumpParser
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read app_name string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("BacaDulu", appName)
  }

  @Test
  fun `cleanSiteUrl strips protocols and trailing slash`() {
    val raw = "https://kuliner-nusantara.net/"
    val cleaned = WordPressDumpParser.cleanSiteUrl(raw)
    assertEquals("kuliner-nusantara.net", cleaned)
  }
}
