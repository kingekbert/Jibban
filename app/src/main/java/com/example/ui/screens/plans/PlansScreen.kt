package com.example.ui.screens.plans

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.components.AppSegmentedControl
import com.example.ui.screens.cheques.ChequesScreen
import com.example.ui.screens.cheques.ChequesViewModel
import com.example.ui.screens.recurring.RecurringTransactionsScreen
import com.example.ui.screens.recurring.RecurringTransactionsViewModel
import com.example.ui.screens.savings.SavingsGoalsScreen
import com.example.ui.screens.savings.SavingsGoalsViewModel

data class PlanTabItem(
    val title: String,
    val icon: ImageVector
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlansScreen(
    recurringViewModel: RecurringTransactionsViewModel,
    chequesViewModel: ChequesViewModel,
    savingsGoalsViewModel: SavingsGoalsViewModel,
    onNavigateToAddRecurring: () -> Unit,
    onNavigateToEditRecurring: (Long) -> Unit = {},
    onNavigateToAddCheque: () -> Unit,
    onNavigateToEditCheque: (Long) -> Unit = {},
    onNavigateToAddSavingsGoal: () -> Unit,
    onNavigateToEditSavingsGoal: (Long) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }

    val tabs = listOf(
        PlanTabItem(title = "اقساط", icon = Icons.Default.Repeat),
        PlanTabItem(title = "چک‌ها", icon = Icons.Default.ReceiptLong),
        PlanTabItem(title = "پس‌انداز", icon = Icons.Default.Savings)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "برنامه‌ها",
                        fontWeight = FontWeight.Bold
                    )
                }
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AppSegmentedControl(
                items = tabs.map { it.title },
                itemIcons = tabs.map { it.icon },
                selectedIndex = selectedTabIndex,
                onSelect = { selectedTabIndex = it },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            when (selectedTabIndex) {
                0 -> {
                    RecurringTransactionsScreen(
                        viewModel = recurringViewModel,
                        onNavigateBack = null,
                        onNavigateToAdd = onNavigateToAddRecurring,
                        onNavigateToEdit = onNavigateToEditRecurring,
                        showTopBar = false,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    )
                }
                1 -> {
                    ChequesScreen(
                        viewModel = chequesViewModel,
                        onNavigateBack = null,
                        onNavigateToAdd = onNavigateToAddCheque,
                        onNavigateToEdit = onNavigateToEditCheque,
                        showTopBar = false,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    )
                }
                2 -> {
                    SavingsGoalsScreen(
                        viewModel = savingsGoalsViewModel,
                        onNavigateBack = null,
                        onNavigateToAdd = onNavigateToAddSavingsGoal,
                        onNavigateToEdit = onNavigateToEditSavingsGoal,
                        showTopBar = false,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    )
                }
            }
        }
    }
}
