package dev.mcphub.acp.hub.tools

import android.content.ContentProviderOperation
import android.content.Context
import android.provider.ContactsContract
import dev.mcphub.acp.hub.server.HubTool
import dev.mcphub.acp.hub.server.ToolContent
import dev.mcphub.acp.hub.server.ToolResult
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/**
 * Tool for searching contacts by name.
 */
class ContactsSearchTool(private val context: Context) : HubTool {

    override val name: String = "contacts.search"

    override val description: String = "Search contacts by name and return matching names and phone numbers"

    override val inputSchema: JsonObject = buildJsonObject {
        put("type", "object")
        put("properties", buildJsonObject {
            put("query", buildJsonObject {
                put("type", "string")
                put("description", "The name to search for in contacts")
            })
        })
        put("required", buildJsonArray {
            add(kotlinx.serialization.json.JsonPrimitive("query"))
        })
    }

    override suspend fun execute(params: JsonObject): ToolResult {
        val query = params["query"]?.jsonPrimitive?.content
            ?: return ToolResult(
                content = listOf(ToolContent.TextContent("Missing required parameter: query")),
                isError = true
            )

        return try {
            val contacts = buildJsonArray {
                val cursor = context.contentResolver.query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    arrayOf(
                        ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                        ContactsContract.CommonDataKinds.Phone.NUMBER
                    ),
                    "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?",
                    arrayOf("%$query%"),
                    "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"
                )

                cursor?.use {
                    while (it.moveToNext()) {
                        val name = it.getString(
                            it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                        )
                        val phoneNumber = it.getString(
                            it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)
                        )
                        add(buildJsonObject {
                            put("name", name)
                            put("phone_number", phoneNumber)
                        })
                    }
                }
            }

            ToolResult(
                content = listOf(ToolContent.TextContent(contacts.toString()))
            )
        } catch (e: SecurityException) {
            ToolResult(
                content = listOf(ToolContent.TextContent("Permission denied: READ_CONTACTS permission is required")),
                isError = true
            )
        } catch (e: Exception) {
            ToolResult(
                content = listOf(ToolContent.TextContent("Failed to search contacts: ${e.message}")),
                isError = true
            )
        }
    }
}

/**
 * Tool for creating a new contact.
 */
class ContactsCreateTool(private val context: Context) : HubTool {

    override val name: String = "contacts.create"

    override val description: String = "Create a new contact with name, phone number, and email"

    override val inputSchema: JsonObject = buildJsonObject {
        put("type", "object")
        put("properties", buildJsonObject {
            put("name", buildJsonObject {
                put("type", "string")
                put("description", "The contact's display name")
            })
            put("phone_number", buildJsonObject {
                put("type", "string")
                put("description", "The contact's phone number (optional)")
            })
            put("email", buildJsonObject {
                put("type", "string")
                put("description", "The contact's email address (optional)")
            })
        })
        put("required", buildJsonArray {
            add(kotlinx.serialization.json.JsonPrimitive("name"))
        })
    }

    override suspend fun execute(params: JsonObject): ToolResult {
        val name = params["name"]?.jsonPrimitive?.content
            ?: return ToolResult(
                content = listOf(ToolContent.TextContent("Missing required parameter: name")),
                isError = true
            )

        val phoneNumber = params["phone_number"]?.jsonPrimitive?.content
        val email = params["email"]?.jsonPrimitive?.content

        return try {
            val operations = ArrayList<ContentProviderOperation>()

            // Insert raw contact
            operations.add(
                ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
                    .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
                    .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null)
                    .build()
            )

            // Insert display name
            operations.add(
                ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                    .withValue(
                        ContactsContract.Data.MIMETYPE,
                        ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE
                    )
                    .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, name)
                    .build()
            )

            // Insert phone number if provided
            if (phoneNumber != null) {
                operations.add(
                    ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                        .withValue(
                            ContactsContract.Data.MIMETYPE,
                            ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE
                        )
                        .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, phoneNumber)
                        .withValue(
                            ContactsContract.CommonDataKinds.Phone.TYPE,
                            ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE
                        )
                        .build()
                )
            }

            // Insert email if provided
            if (email != null) {
                operations.add(
                    ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                        .withValue(
                            ContactsContract.Data.MIMETYPE,
                            ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE
                        )
                        .withValue(ContactsContract.CommonDataKinds.Email.DATA, email)
                        .withValue(
                            ContactsContract.CommonDataKinds.Email.TYPE,
                            ContactsContract.CommonDataKinds.Email.TYPE_HOME
                        )
                        .build()
                )
            }

            context.contentResolver.applyBatch(ContactsContract.AUTHORITY, operations)

            ToolResult(
                content = listOf(ToolContent.TextContent("Contact '$name' created successfully"))
            )
        } catch (e: SecurityException) {
            ToolResult(
                content = listOf(ToolContent.TextContent("Permission denied: WRITE_CONTACTS permission is required")),
                isError = true
            )
        } catch (e: Exception) {
            ToolResult(
                content = listOf(ToolContent.TextContent("Failed to create contact: ${e.message}")),
                isError = true
            )
        }
    }
}
