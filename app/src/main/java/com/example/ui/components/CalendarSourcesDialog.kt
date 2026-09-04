package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.DeviceCalendarSource
import com.example.data.DeviceCalendars
import com.example.ui.theme.PulseMint

@Composable
fun CalendarSourcesDialog(
    calendars: List<DeviceCalendarSource>,
    selectedIds: Set<Long>,
    onConfirm: (Set<Long>) -> Unit,
    onDismiss: () -> Unit
) {
    val suggested = remember(calendars) { DeviceCalendars.suggestedIds(calendars) }
    var checked by remember(calendars, selectedIds) {
        mutableStateOf(
            if (selectedIds.isNotEmpty()) selectedIds
            else suggested
        )
    }
    val grouped = remember(calendars) {
        calendars.groupBy { it.accountName.ifBlank { "This device" } }
    }

    Dialog(onDismissRequest = onDismiss) {
        PulseSurfaceCard(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("calendar_sources_dialog")
        ) {
            Text(
                text = "Choose calendars",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = pulseInk()
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Pick which phone calendars to show. Uncheck Holidays and extra accounts so events don’t appear twice.",
                fontSize = 13.sp,
                color = pulseMuted()
            )
            Spacer(modifier = Modifier.height(14.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 360.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                grouped.forEach { (account, list) ->
                    Text(
                        text = account,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = pulseMuted(),
                        modifier = Modifier.padding(top = 8.dp, bottom = 6.dp)
                    )
                    list.forEach { calendar ->
                        val on = calendar.id in checked
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                                .testTag("calendar_source_${calendar.id}"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (calendar.color != 0) Color(calendar.color)
                                        else PulseMint
                                    )
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = calendar.displayName,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = pulseInk(),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (calendar.isPrimary) {
                                    Text(
                                        text = "Primary",
                                        fontSize = 11.sp,
                                        color = pulseMuted()
                                    )
                                }
                            }
                            PixiToggle(
                                checked = on,
                                onCheckedChange = {
                                    checked = if (it) checked + calendar.id else checked - calendar.id
                                }
                            )
                        }
                    }
                }
                if (calendars.isEmpty()) {
                    Text(
                        text = "No visible calendars on this phone yet.",
                        fontSize = 13.sp,
                        color = pulseMuted()
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PixiOutlineButton(
                    text = "Suggested",
                    onClick = { checked = suggested },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("calendar_sources_suggested")
                )
                PixiPrimaryButton(
                    text = "Continue",
                    onClick = { onConfirm(checked) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("calendar_sources_confirm")
                )
            }
        }
    }
}
