package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.model.AppLanguage
import com.example.model.IslamicSection
import com.example.model.NewsCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ExampleRobolectricTest {

    @Test
    fun testAppNameIsKhabarnama() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertEquals("Khabarnama", context.getString(R.string.app_name))
    }

    @Test
    fun testAppCategories() {
        val islamic = NewsCategory.ISLAMIC
        assertEquals("Islamic", islamic.getLabel(AppLanguage.ENGLISH))
        assertEquals("اسلامی", islamic.getLabel(AppLanguage.URDU))
        assertEquals("All", NewsCategory.ALL.getLabel(AppLanguage.ENGLISH))
        assertEquals("تمام", NewsCategory.ALL.getLabel(AppLanguage.URDU))
    }

    @Test
    fun testIslamicSections() {
        assertNotNull(IslamicSection.HAJJ_UMRAH)
        assertEquals("Hajj & Umrah", IslamicSection.HAJJ_UMRAH.getLabel(AppLanguage.ENGLISH))
        assertEquals("حج و عمرہ", IslamicSection.HAJJ_UMRAH.getLabel(AppLanguage.URDU))
    }
}
