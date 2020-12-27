package top.orange233.musicmaker.bean

import java.io.File

class AudioClip {
    var name: String? = null
    var time: Int? = null

}

class CutTime constructor() {
    var startTime: Int? = null
    var endTime: Int? = null

    constructor(starTime: Int, endTime: Int) : this() {
        this.startTime = starTime
        this.endTime = endTime
    }
}

class Audio {
    var name: String? = null
    var fileName: String? = null
    var dir: String? = null
    var path: String? = null
    var file: File? = null
    var delayTime: Int? = null
}