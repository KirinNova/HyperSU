package com.sukisu.ultra.ui.component.choosekmidialog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sukisu.ultra.R
import com.sukisu.ultra.ui.component.folk.FolkAlertDialog
import com.sukisu.ultra.ui.component.folk.FolkButtonDefaults
import com.sukisu.ultra.ui.component.folk.FolkSelectableRow
import com.sukisu.ultra.ui.theme.tokens.FolkType
import com.sukisu.ultra.ui.util.getCurrentKmi
import com.sukisu.ultra.ui.util.getSupportedKmis

/**
 * The KMI chooser, in the FolkPatch design: the supported KMIs as radio rows,
 * with the device's current KMI marked.
 *
 * Behaviour is unchanged - the list and the current value are still probed
 * asynchronously, cancel restores the detected value, and confirm is only
 * enabled for a supported choice.
 */
@Composable
fun ChooseKmiDialog(
    show: Boolean,
    onDismissRequest: () -> Unit,
    onSelected: (String?) -> Unit,
) {
    if (!show) return

    val supportedKMIs by produceState(initialValue = emptyList<String>()) {
        value = getSupportedKmis()
    }

    val currentKmi by produceState(initialValue = "") {
        value = getCurrentKmi()
    }

    val selectedKmi = remember(currentKmi) { mutableStateOf(currentKmi) }

    FolkAlertDialog(
        onDismissRequest = {
            onDismissRequest()
            selectedKmi.value = currentKmi
        },
        width = 340.dp,
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text(
                text = stringResource(R.string.select_kmi),
                style = FolkType.Title,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 8.dp),
            )

            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                supportedKMIs.forEach { kmi ->
                    FolkSelectableRow(
                        title = kmi,
                        summary = if (kmi == currentKmi) stringResource(R.string.current_device_kmi) else null,
                        selected = selectedKmi.value == kmi,
                        onClick = { selectedKmi.value = kmi },
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(
                    onClick = {
                        onDismissRequest()
                        selectedKmi.value = currentKmi
                    },
                    colors = FolkButtonDefaults.textColors(),
                ) {
                    Text(stringResource(android.R.string.cancel))
                }

                Button(
                    onClick = {
                        onSelected(selectedKmi.value)
                        onDismissRequest()
                    },
                    enabled = supportedKMIs.contains(selectedKmi.value),
                    colors = FolkButtonDefaults.filledColors(),
                    modifier = Modifier.padding(start = 8.dp),
                ) {
                    Text(stringResource(R.string.confirm))
                }
            }
        }
    }
}
