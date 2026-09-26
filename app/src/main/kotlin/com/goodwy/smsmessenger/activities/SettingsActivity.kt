        }
    }

    private fun setupPersianFont() = binding.apply {
        settingsPersianFont.text = getPersianFontText()

        settingsPersianFontHolder.setOnClickListener {
            val items = arrayListOf(
                RadioItem(FONT_TYPE_SYSTEM_DEFAULT, getString(R.string.persian_font_system_default)),
                RadioItem(FONT_TYPE_MONOSPACE, getString(com.goodwy.strings.R.string.monospace)),
                RadioItem(FONT_TYPE_PERSIAN_VAZIRMATN, getString(R.string.persian_font_vazirmatn)),
                RadioItem(FONT_TYPE_PERSIAN_SAHEL, getString(R.string.persian_font_sahel)),
                RadioItem(FONT_TYPE_PERSIAN_SHABNAM, getString(R.string.persian_font_shabnam)),
                RadioItem(FONT_TYPE_PERSIAN_SAMIM, getString(R.string.persian_font_samim)),
                RadioItem(FONT_TYPE_PERSIAN_TANHA, getString(R.string.persian_font_tanha)),
                RadioItem(FONT_TYPE_PERSIAN_NAHID, getString(R.string.persian_font_nahid))
            )

            RadioGroupDialog(
                this@SettingsActivity,
                items,
                baseConfig.fontType,
                R.string.persian_font
            ) {
                baseConfig.fontType = it as Int
                if (baseConfig.fontType != FONT_TYPE_CUSTOM) {
                    baseConfig.fontName = ""
                }
                FontHelper.clearCache()
                settingsPersianFont.text = getPersianFontText()
                recreate()
            }
        }
    }

    private fun getPersianFontText() = getString(
        when (baseConfig.fontType) {
            FONT_TYPE_PERSIAN_VAZIRMATN -> R.string.persian_font_vazirmatn
            FONT_TYPE_PERSIAN_SAHEL -> R.string.persian_font_sahel
            FONT_TYPE_PERSIAN_SHABNAM -> R.string.persian_font_shabnam
            FONT_TYPE_PERSIAN_SAMIM -> R.string.persian_font_samim
            FONT_TYPE_PERSIAN_TANHA -> R.string.persian_font_tanha
            FONT_TYPE_PERSIAN_NAHID -> R.string.persian_font_nahid
            FONT_TYPE_MONOSPACE -> com.goodwy.commons.R.string.monospace
            FONT_TYPE_CUSTOM -> com.goodwy.commons.R.string.custom
            else -> R.string.persian_font_system_default
        }
    )

    private fun setupCustomizeNotifications() = binding.apply {
        if (settingsCustomizeNotificationsHolder.isGone()) {