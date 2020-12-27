package top.orange233.musicmaker.controller

import com.google.gson.Gson
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import top.orange233.musicmaker.bean.Audio
import top.orange233.musicmaker.bean.AudioClip
import top.orange233.musicmaker.bean.CutTime
import top.orange233.musicmaker.util.AudioMaker
import java.io.*
import javax.servlet.http.HttpServletResponse

@RestController
class MusicController {
    private val logger = LoggerFactory.getLogger(this.javaClass)

    @PostMapping("/music/merge")
    fun mergeMusicClips(@RequestBody audioClips: List<AudioClip>, response: HttpServletResponse) {
        logger.info("Receive POST in /music/merge")
        if (audioClips.isEmpty()) {
            response.status = 404
            return
        }

        val gson = Gson()
        logger.info("RequestBody: ${gson.toJson(audioClips)}")

        val audioMaker = AudioMaker()
        val audios = mutableListOf<Audio>()
        for (audioClip in audioClips) {
            audios.add(Audio().apply {
                audioMaker.updateAudioInfo(this, audioClip.name!!)
                delayTime = audioClip.time
            })
        }
        audioMaker.mergeAudios(audios)
        response.contentType = "application/json"
        try {
            val writer = response.writer
            val map = mapOf("data" to audioMaker.id, "statusCode" to 200)
            writer.write(gson.toJson(map))
        } catch (e: IOException) {
            logger.trace(e.stackTraceToString())
        }
        logger.info("Done ${object {}.javaClass.enclosingMethod.name}.")
    }

    @GetMapping("/music/merge/{id}")
    fun getMergedAudio(@PathVariable("id") id: String, response: HttpServletResponse) {
        logger.info("Receive GET in /music/merge/$id")
        val audioMaker = AudioMaker(id)
        if (!audioMaker.resultAudio.file!!.exists()) {
            response.status = 404
            return
        }

        try {
            val file = audioMaker.resultAudio.file!!
            val inputStream = FileInputStream(file)
            val data = ByteArray(file.length().toInt())
            inputStream.read(data)
            inputStream.close()
            response.contentType = "audio/mpeg"
            response.addHeader("Content-Length", data.size.toString())
            val outputStream: OutputStream = response.outputStream
            outputStream.write(data)
            outputStream.flush()
            outputStream.close()
        } catch (e: IOException) {
            logger.trace(e.stackTraceToString())
        }
        logger.info("Done ${object {}.javaClass.enclosingMethod.name}.")
    }

    @PostMapping("/music/cut")
    fun cutAudio(
        @RequestParam("myfile") multipartFile: MultipartFile,
        @RequestParam("startTime") startTime: Int,
        @RequestParam("endTime") endTime: Int,
        response: HttpServletResponse
    ) {
        logger.info("Receive POST in /music/cut")
        logger.info("startTime is $startTime")
        val gson = Gson()
        val audioMaker = AudioMaker()

        // val multipartFile = multipartFiles[0]
        if (!multipartFile.isEmpty) {
            try {
                val outputStream = BufferedOutputStream(FileOutputStream(audioMaker.resultAudio.file!!))
                val data = multipartFile.bytes
                outputStream.write(data)
                outputStream.close()
            } catch (e: IOException) {
                logger.trace(e.stackTraceToString())
            }
        }

        audioMaker.cutAudio(audioMaker.resultAudio, CutTime(startTime, endTime))

        response.contentType = "application/json"
        try {
            val writer = response.writer
            val map = mapOf("data" to audioMaker.id, "statusCode" to 200)
            writer.write(gson.toJson(map))
        } catch (e: IOException) {
            logger.trace(e.stackTraceToString())
        }
        logger.info("Done ${object {}.javaClass.enclosingMethod.name}.")
    }

    @GetMapping("/music/cut/{id}")
    fun getCutAudio(@PathVariable("id") id: String, response: HttpServletResponse) {
        logger.info("Receive GET in /music/cut/$id")
        val audioMaker = AudioMaker(id)
        if (!audioMaker.resultAudio.file!!.exists()) {
            response.status = 404
            return
        }

        try {
            val file = audioMaker.resultAudio.file!!
            val inputStream = FileInputStream(file)
            val data = ByteArray(file.length().toInt())
            inputStream.read(data)
            inputStream.close()
            response.contentType = "audio/mpeg"
            response.addHeader("Content-Length", data.size.toString())
            val outputStream: OutputStream = response.outputStream
            outputStream.write(data)
            outputStream.flush()
            outputStream.close()
        } catch (e: IOException) {
            logger.trace(e.stackTraceToString())
        }
        logger.info("Done ${object {}.javaClass.enclosingMethod.name}.")
    }
}