package com.finanzas.personales.data.local.database

import androidx.room.TypeConverter
import com.finanzas.personales.data.local.entities.ExpenseCategory

class Converters {
    @TypeConverter
    fun fromExpenseCategory(category: ExpenseCategory): String {
        return category.name
    }

    @TypeConverter
    fun toExpenseCategory(value: String): ExpenseCategory {
        return ExpenseCategory.valueOf(value)
    }
}
