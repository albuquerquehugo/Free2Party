package com.example.free2party.ui.components.dialogs

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.free2party.R
import com.example.free2party.data.model.UserSearchResult
import com.example.free2party.ui.components.InputTextField
import com.example.free2party.ui.screens.home.InviteFriendUiState

@Composable
fun InviteFriendDialog(
    query: String,
    onQueryChange: (String) -> Unit,
    searchResults: List<UserSearchResult>,
    isSearching: Boolean,
    onUserSelected: (UserSearchResult) -> Unit,
    onDismiss: () -> Unit,
    uiState: InviteFriendUiState
) {
    BaseDialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth()
        ) {
            Text(
                text = stringResource(R.string.title_invite_friend),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            InputTextField(
                value = query,
                onValueChange = onQueryChange,
                label = stringResource(R.string.title_search),
                placeholder = stringResource(R.string.text_placeholder_search_user),
                icon = Icons.Default.Search,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            Box(modifier = Modifier.heightIn(max = 450.dp)) {
                if (isSearching) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    }
                } else if (query.isNotBlank() && searchResults.isEmpty()) {
                    Text(
                        text = stringResource(R.string.text_no_results_found),
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    LazyColumn {
                        items(searchResults) { user ->
                            ListItem(
                                headlineContent = { Text(user.fullName) },
                                supportingContent = { Text(user.email) },
                                modifier = Modifier.clickable { onUserSelected(user) }
                            )
                        }
                    }
                }
            }

            if (uiState is InviteFriendUiState.Error) {
                Text(
                    text = uiState.message.asString(),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.button_cancel))
                }
            }
        }
    }
}
