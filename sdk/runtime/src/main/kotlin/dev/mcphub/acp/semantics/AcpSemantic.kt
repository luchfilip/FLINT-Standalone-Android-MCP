package dev.mcphub.acp.semantics

import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.semantics

object AcpSemantic {
    val ContentKey = SemanticsPropertyKey<String>("AcpContentKey")
    val ListId = SemanticsPropertyKey<String>("AcpListId")
    val ListDescription = SemanticsPropertyKey<String>("AcpListDescription")
    val ItemIndex = SemanticsPropertyKey<Int>("AcpItemIndex")
    val ActionName = SemanticsPropertyKey<String>("AcpActionName")
    val ActionDescription = SemanticsPropertyKey<String>("AcpActionDescription")
    val OverlayId = SemanticsPropertyKey<String>("AcpOverlayId")
    val OverlayDescription = SemanticsPropertyKey<String>("AcpOverlayDescription")
}

var SemanticsPropertyReceiver.acpContentKey by AcpSemantic.ContentKey
var SemanticsPropertyReceiver.acpListId by AcpSemantic.ListId
var SemanticsPropertyReceiver.acpListDescription by AcpSemantic.ListDescription
var SemanticsPropertyReceiver.acpItemIndex by AcpSemantic.ItemIndex
var SemanticsPropertyReceiver.acpActionName by AcpSemantic.ActionName
var SemanticsPropertyReceiver.acpActionDescription by AcpSemantic.ActionDescription
var SemanticsPropertyReceiver.acpOverlayId by AcpSemantic.OverlayId
var SemanticsPropertyReceiver.acpOverlayDescription by AcpSemantic.OverlayDescription

fun Modifier.acpContent(key: String) = this.semantics {
    acpContentKey = key
}

fun Modifier.acpList(id: String, description: String = "") = this.semantics {
    acpListId = id
    acpListDescription = description
}

fun Modifier.acpItem(index: Int) = this.semantics {
    acpItemIndex = index
}

fun Modifier.acpAction(name: String, description: String = "") = this.semantics {
    acpActionName = name
    acpActionDescription = description
}

fun Modifier.acpOverlay(id: String, description: String = "") = this.semantics {
    acpOverlayId = id
    acpOverlayDescription = description
}
