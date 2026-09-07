package com.bankpoker.app.ui.components

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bankpoker.app.ui.theme.Cream
import com.bankpoker.app.ui.theme.Gold

@Composable
fun GroupCodeChip(
    code: String,
    groupName: String = "Poker Group",
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    fun copyToClipboard() {
        clipboardManager.setText(AnnotatedString(code))
        Toast.makeText(context, "Code copied: $code", Toast.LENGTH_SHORT).show()
    }

    fun shareInvite() {
        val shareText = "Join our poker group \"$groupName\" on BankPoker!\nInvite Code: $code\nJoin link: https://bankjoker.ir/#/join/$code"
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "BankPoker Group Invite")
            putExtra(Intent.EXTRA_TEXT, shareText)
        }
        context.startActivity(Intent.createChooser(intent, "Share Group Code"))
    }

    Surface(
        modifier = modifier
            .clickable { copyToClipboard() },
        shape = RoundedCornerShape(8.dp),
        color = Gold.copy(alpha = 0.15f),
        border = BorderStroke(1.dp, Gold.copy(alpha = 0.6f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = code.uppercase(),
                color = Cream,
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.5.sp
            )

            Icon(
                imageVector = Icons.Default.ContentCopy,
                contentDescription = "Copy code",
                tint = Gold,
                modifier = Modifier.size(13.dp)
            )

            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clickable { shareInvite() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = "Share invite",
                    tint = Gold,
                    modifier = Modifier.size(13.dp)
                )
            }
        }
    }
}
