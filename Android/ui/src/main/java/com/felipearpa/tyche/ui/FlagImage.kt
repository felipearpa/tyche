package com.felipearpa.tyche.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import coil3.compose.AsyncImage

@Composable
fun FlagImage(
    teamId: String,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
) {
    flagResource(teamId)?.let { resourceId ->
        AsyncImage(
            model = resourceId,
            contentDescription = contentDescription,
            modifier = modifier,
        )
    }
}

private fun flagResource(teamId: String): Int? = when (teamId.lowercase()) {
    "ar" -> R.drawable.flag_ar
    "at" -> R.drawable.flag_at
    "au" -> R.drawable.flag_au
    "ba" -> R.drawable.flag_ba
    "be" -> R.drawable.flag_be
    "br" -> R.drawable.flag_br
    "ca" -> R.drawable.flag_ca
    "cd" -> R.drawable.flag_cd
    "ch" -> R.drawable.flag_ch
    "ci" -> R.drawable.flag_ci
    "co" -> R.drawable.flag_co
    "cv" -> R.drawable.flag_cv
    "cw" -> R.drawable.flag_cw
    "cz" -> R.drawable.flag_cz
    "de" -> R.drawable.flag_de
    "dz" -> R.drawable.flag_dz
    "ec" -> R.drawable.flag_ec
    "eg" -> R.drawable.flag_eg
    "es" -> R.drawable.flag_es
    "fr" -> R.drawable.flag_fr
    "gb_eng" -> R.drawable.flag_gb_eng
    "gb_sct" -> R.drawable.flag_gb_sct
    "gh" -> R.drawable.flag_gh
    "hr" -> R.drawable.flag_hr
    "ht" -> R.drawable.flag_ht
    "iq" -> R.drawable.flag_iq
    "ir" -> R.drawable.flag_ir
    "jo" -> R.drawable.flag_jo
    "jp" -> R.drawable.flag_jp
    "kr" -> R.drawable.flag_kr
    "ma" -> R.drawable.flag_ma
    "mx" -> R.drawable.flag_mx
    "nl" -> R.drawable.flag_nl
    "no" -> R.drawable.flag_no
    "nz" -> R.drawable.flag_nz
    "pa" -> R.drawable.flag_pa
    "pt" -> R.drawable.flag_pt
    "py" -> R.drawable.flag_py
    "qa" -> R.drawable.flag_qa
    "sa" -> R.drawable.flag_sa
    "se" -> R.drawable.flag_se
    "sn" -> R.drawable.flag_sn
    "tn" -> R.drawable.flag_tn
    "tr" -> R.drawable.flag_tr
    "us" -> R.drawable.flag_us
    "uy" -> R.drawable.flag_uy
    "uz" -> R.drawable.flag_uz
    "za" -> R.drawable.flag_za
    else -> null
}
