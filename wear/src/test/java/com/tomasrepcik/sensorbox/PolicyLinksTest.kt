package com.tomasrepcik.sensorbox

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

class PolicyLinksTest {
    @Test
    fun `policy links match the public SensorBox policy pages`() {
        val strings = DocumentBuilderFactory.newInstance()
            .newDocumentBuilder()
            .parse(File("src/main/res/values/strings.xml"))
            .getElementsByTagName("string")
        val policyLinks = buildMap {
            for (index in 0 until strings.length) {
                val resource = strings.item(index)
                val name = resource.attributes.getNamedItem("name")?.nodeValue ?: continue
                if (name == "link_privacy_policy" || name == "link_terms") {
                    put(name, resource.textContent.trim())
                }
            }
        }

        assertEquals(
            "https://tomasrepcik.dev/sensorbox/privacy-policy",
            policyLinks["link_privacy_policy"],
        )
        assertEquals(
            "https://tomasrepcik.dev/sensorbox/terms-of-use",
            policyLinks["link_terms"],
        )
    }
}
