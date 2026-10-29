package es.igarciadelamo.txoka.ui.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import es.igarciadelamo.txoka.R
import es.igarciadelamo.txoka.domain.profile.PhoneCountries
import es.igarciadelamo.txoka.domain.profile.PhoneCountry

private val ListMaxHeight = 420.dp

@Composable
fun CountryPickerDialog(selected: PhoneCountry, onSelect: (PhoneCountry) -> Unit, onDismiss: () -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    val countries = remember(query) { filterCountries(query) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = 6.dp,
        ) {
            Column(modifier = Modifier.padding(vertical = 16.dp)) {
                Text(
                    text = stringResource(R.string.phone_country),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(horizontal = 24.dp),
                )
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                    placeholder = { Text(text = stringResource(R.string.phone_country_search)) },
                    singleLine = true,
                )
                LazyColumn(modifier = Modifier.heightIn(max = ListMaxHeight)) {
                    items(items = countries, key = { country -> country.iso }) { country ->
                        ListItem(
                            headlineContent = { Text(text = "${country.flag}  ${country.label}") },
                            supportingContent = { Text(text = "+${country.dialCode}") },
                            trailingContent = if (country == selected) {
                                {
                                    Icon(
                                        imageVector = Icons.Filled.Check,
                                        contentDescription = null,
                                    )
                                }
                            } else {
                                null
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSelect(country)
                                    onDismiss()
                                },
                        )
                    }
                }
            }
        }
    }
}

private fun filterCountries(query: String): List<PhoneCountry> {
    val text = query.trim()
    val sorted = PhoneCountries.all.sortedBy { country -> country.label }
    if (text.isEmpty()) return sorted
    val digits = text.removePrefix("+")
    return sorted.filter { country ->
        country.label.contains(text, ignoreCase = true) ||
            country.iso.contains(text, ignoreCase = true) ||
            (digits.isNotEmpty() && country.dialCode.startsWith(digits))
    }
}
