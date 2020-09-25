package com.huawei.gameservice

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.huawei.hms.common.ApiException
import com.huawei.hms.jos.JosApps
import com.huawei.hms.jos.games.GameScopes
import com.huawei.hms.jos.games.Games
import com.huawei.hms.jos.games.GamesStatusCodes
import com.huawei.hms.jos.games.archive.ArchiveDetails
import com.huawei.hms.jos.games.archive.ArchiveSummaryUpdate
import com.huawei.hms.support.api.entity.auth.Scope
import com.huawei.hms.support.hwid.HuaweiIdAuthManager
import com.huawei.hms.support.hwid.request.HuaweiIdAuthParams
import com.huawei.hms.support.hwid.request.HuaweiIdAuthParamsHelper
import com.huawei.hms.support.hwid.result.AuthHuaweiId
import com.huawei.hms.support.hwid.result.HuaweiIdAuthResult
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*
import kotlin.random.Random

class MainActivity : AppCompatActivity(), View.OnClickListener {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initView()
    }

    private fun initView() {
        login.setOnClickListener(this)
        save.setOnClickListener(this)
        load.setOnClickListener(this)
    }

    private fun init() {
        val appsClient = JosApps.getJosAppsClient(this, SignInCenter.authHuaweiId)
        appsClient.init()
    }

    private fun signIn() {
        val authHuaweiIdTask = HuaweiIdAuthManager.getService(this, SignInCenter.huaweiIdParams).silentSignIn()
        authHuaweiIdTask.addOnSuccessListener { authHuaweiId ->
            Toast.makeText(applicationContext, authHuaweiId.displayName, Toast.LENGTH_SHORT).show()
            SignInCenter.get().updateAuthHuaweiId(authHuaweiId)
        }.addOnFailureListener { e ->
            if (e is ApiException) {
                Toast.makeText(applicationContext, getString(R.string.onfail) + getString(R.string.onfail) + e.statusCode, Toast.LENGTH_SHORT).show()
                signInNewWay()
            }
        }
    }

    fun signInNewWay() {
        val intent = HuaweiIdAuthManager.getService(this@MainActivity, SignInCenter.huaweiIdParams).signInIntent
        startActivityForResult(intent, SIGN_IN_INTENT)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (SIGN_IN_INTENT == requestCode) {
            handleSignInResult(data)
        }
    }

    private fun handleSignInResult(data: Intent?) {
        if (null == data) {
            return
        }
        val jsonSignInResult = data.getStringExtra("HUAWEIID_SIGNIN_RESULT")
        try {
            val signInResult = HuaweiIdAuthResult().fromJson(jsonSignInResult)
            if (0 == signInResult.status.statusCode) {
                SignInCenter.get().updateAuthHuaweiId(signInResult.huaweiId)
            } else {
                Toast.makeText(applicationContext, getString(R.string.onfail) + signInResult.status.statusCode, Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
        }
    }


    override fun onClick(view: View) {
        when (view.id) {
            R.id.load -> getArchive()
            R.id.save -> clearArchives()
            R.id.login -> {
                init()
                signIn()
            }
            else -> {
            }
        }
    }


    private fun getArchive() {
        val task = Games.getArchiveClient(this, SignInCenter.authHuaweiId).getArchiveSummaryList(true)
        task?.addOnSuccessListener { archiveSummaries ->
            val archiveSummary = archiveSummaries.get(archiveSummaries.lastIndex)
            text.text = getString(R.string.time, archiveSummary.activeTime)
        }
    }

    private fun addArchive() {
        val description = getString(R.string.description)
        val progress = Random.nextLong(level) //Случайный уровень игрока
        var playedTime = edit.getText().toString().toLong()
        val builder = ArchiveSummaryUpdate.Builder().setActiveTime(playedTime)
                .setCurrentProgress(progress)
                .setDescInfo(description)
        val archiveMetadataChange = builder.build()
        val archiveContents = ArchiveDetails.Builder().build()
        archiveContents.set((progress.toString() + description + playedTime).toByteArray())
        val task = Games.getArchiveClient(this, SignInCenter.authHuaweiId).addArchive(archiveContents, archiveMetadataChange, true)
        task?.addOnSuccessListener { archiveSummary ->
            if (archiveSummary != null) {
                Toast.makeText(applicationContext, getString(R.string.onsaved), Toast.LENGTH_SHORT).show()
            }
        }?.addOnFailureListener { e ->
            val apiException = e as ApiException
            val content = apiException.statusCode.toString()
            Toast.makeText(applicationContext, content, Toast.LENGTH_SHORT).show()
            if (apiException.statusCode == GamesStatusCodes.GAME_STATE_ARCHIVE_NO_DRIVE) {
            }
        }
    }

    private fun clearArchives() {
        val task = Games.getArchiveClient(this, SignInCenter.authHuaweiId).getArchiveSummaryList(true)
        task?.addOnSuccessListener { archiveSummaries ->
            if (archiveSummaries != null) {
                for (i in 0..archiveSummaries.lastIndex) {
                    val task1 = Games.getArchiveClient(this, SignInCenter.authHuaweiId).removeArchive(archiveSummaries.get(i))
                    task1?.addOnSuccessListener { id ->
                        if (i == archiveSummaries.lastIndex) addArchive()
                    }?.addOnFailureListener {}
                }
            } else {
                addArchive()
            }
        }

    }

    companion object {
        private const val SIGN_IN_INTENT = 3000
        private const val level: Long = 101
    }

    object SignInCenter {
        var authHuaweiId: AuthHuaweiId? = null

        fun updateAuthHuaweiId(AuthHuaweiId: AuthHuaweiId?) {
            authHuaweiId = AuthHuaweiId
        }

        fun get(): SignInCenter {
            return this
        }

        val huaweiIdParams: HuaweiIdAuthParams
            get() {
                val scopes: MutableList<Scope> = ArrayList()
                scopes.add(GameScopes.DRIVE_APP_DATA)
                return HuaweiIdAuthParamsHelper(HuaweiIdAuthParams.DEFAULT_AUTH_REQUEST_PARAM_GAME).setScopeList(scopes).createParams()
            }
    }
}