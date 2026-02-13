package com.flintsdk.semantics

import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.semantics

object FlintSemantic {
    val ContentKey = SemanticsPropertyKey<String>("FlintContentKey")
    val ListId = SemanticsPropertyKey<String>("FlintListId")
    val ListDescription = SemanticsPropertyKey<String>("FlintListDescription")
    val ItemIndex = SemanticsPropertyKey<Int>("FlintItemIndex")
    val ActionName = SemanticsPropertyKey<String>("FlintActionName")
    val ActionDescription = SemanticsPropertyKey<String>("FlintActionDescription")
    val OverlayId = SemanticsPropertyKey<String>("FlintOverlayId")
    val OverlayDescription = SemanticsPropertyKey<String>("FlintOverlayDescription")
}

var SemanticsPropertyReceiver.flintContentKey by FlintSemantic.ContentKey
var SemanticsPropertyReceiver.flintListId by FlintSemantic.ListId
var SemanticsPropertyReceiver.flintListDescription by FlintSemantic.ListDescription
var SemanticsPropertyReceiver.flintItemIndex by FlintSemantic.ItemIndex
var SemanticsPropertyReceiver.flintActionName by FlintSemantic.ActionName
var SemanticsPropertyReceiver.flintActionDescription by FlintSemantic.ActionDescription
var SemanticsPropertyReceiver.flintOverlayId by FlintSemantic.OverlayId
var SemanticsPropertyReceiver.flintOverlayDescription by FlintSemantic.OverlayDescription

fun Modifier.flintContent(key: String) = this.semantics {
    flintContentKey = key
}

fun Modifier.flintList(id: String, description: String = "") = this.semantics {
    flintListId = id
    flintListDescription = description
}

fun Modifier.flintItem(index: Int) = this.semantics {
    flintItemIndex = index
}

fun Modifier.flintAction(name: String, description: String = "") = this.semantics {
    flintActionName = name
    flintActionDescription = description
}

fun Modifier.flintOverlay(id: String, description: String = "") = this.semantics {
    flintOverlayId = id
    flintOverlayDescription = description
}
