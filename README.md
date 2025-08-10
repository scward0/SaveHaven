# SaveHaven - Financial Literacy App

An Android app helping young adults (16-22) build healthy financial habits through transaction tracking, data visualization, and educational content.

## Core Features

### Transaction Management
- Manual income/expense logging with predefined categories
- Real-time transaction editing and deletion
- Advanced filtering by type, category, and date range
- Complete transaction history with search capabilities

### Financial Analytics
- Live dashboard with income, expenses, and net savings
- Interactive pie charts for spending/income breakdown
- Motivational status messages based on financial performance
- Recent transactions overview with quick edit access

### Smart Utilities
- Google Maps integration for nearby bank location finder
- GPS-based search and directions to banking services
- Educational tip system with 95+ curated financial advice
- User preferences with notification settings (stored in Firestore)

### Material Design UI
- Consistent navigation drawer across all screens
- Responsive design supporting phones and tablets
- Color-coded transactions (green for income, red for expenses)
- Professional Material Design 3 components throughout

## Project Structure

```
app/src/main/java/com/example/savehaven/
├── MainActivity.kt                    # Entry point with splash screen
├── data/                             # Data layer
│   ├── AuthRepository.kt            # Firebase authentication
│   ├── TransactionRepository.kt     # Transaction CRUD operations
│   ├── Transaction.kt              # Transaction data model
│   └── User.kt                     # User profile model
├── ui/                             # Presentation layer
│   ├── LoginActivity.kt           # User authentication
│   ├── RegisterActivity.kt        # Account creation
│   ├── DashboardActivity.kt       # Main financial overview
│   ├── AddTransactionActivity.kt  # New transaction entry
│   ├── EditTransactionActivity.kt # Transaction modification
│   ├── IncomeActivity.kt          # Income analysis with charts
│   ├── ExpenseActivity.kt         # Expense analysis with charts
│   ├── TransactionHistoryActivity.kt # Full transaction list
│   ├── MapActivity.kt             # Bank finder with Google Maps
│   ├── PreferenceActivity.kt      # User settings
│   ├── PasswordResetActivity.kt   # Password recovery
│   ├── DashboardTransactionAdapter.kt # Recent transactions
│   └── TransactionHistoryAdapter.kt   # Full history list
└── utils/                          # Helper classes
    ├── Constants.kt               # App-wide configuration
    ├── FinancialTipsProvider.kt   # Educational content (95+ tips)
    ├── NavigationHandler.kt       # Centralized navigation logic
    ├── PreferenceHelper.kt        # SharedPreferences management
    └── ValidationUtils.kt          # Input validation utilities
```

## Tech Stack

- **Platform**: Native Android (Kotlin)
- **Authentication**: Firebase Authentication
- **Database**: Cloud Firestore for real-time data sync
- **Maps**: Google Maps API & Places API
- **Charts**: MPAndroidChart for data visualization
- **Local Storage**: SharedPreferences for user settings
- **Architecture**: Repository pattern with MVVM principles


## Setup Requirements

- Android Studio with Kotlin support
- Firebase project with Authentication and Firestore enabled
- Google Maps API key with Maps SDK and Places API
- Minimum Android API level 24 (Android 7.0)
