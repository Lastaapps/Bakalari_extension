package cz.lastaapps.bakalariextension.login

import androidx.lifecycle.ViewModel

class LoginViewModel: ViewModel() {
    var schoolUrlMap: HashMap<String, String> = HashMap()
    var townList: ArrayList<String> = ArrayList()
    var townIndex = -1
    var schoolList: ArrayList<String> = ArrayList()
    var schoolIndex = -1

}