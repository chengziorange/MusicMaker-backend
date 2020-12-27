package top.orange233.musicmaker.util

import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import top.orange233.musicmaker.bean.Audio
import top.orange233.musicmaker.bean.CutTime
import top.orange233.musicmaker.config.*
import java.io.*
import java.util.*


class AudioMaker constructor() {
    var id: String = UUID.randomUUID().toString()
    var resultAudio: Audio = Audio().apply {
        updateAudioInfo(this, DOWNLOADED_FILE_NAME)
        File(dir!!).apply {
            if (!this.exists()) {
                this.mkdir()
            }
        }
    }

    private val logger = LoggerFactory.getLogger(this.javaClass)

    constructor(id: String) : this() {
        this.id = id
        resultAudio.apply {
            updateAudioInfo(this, RESULT_FILE_NAME)
        }
    }

    fun mergeAudios(audios: List<Audio>) {
        val delayedClips = mutableListOf<Audio>()
        runBlocking {
            for (audio in audios) {
                launch(Dispatchers.IO) {
                    delayedClips.add(delayAudio(audio))
                }
            }
        }
        logger.info("Done delay all clips.")
        val dB = when {
            delayedClips.size < 3 -> 2
            delayedClips.size < 10 -> 5
            delayedClips.size < 50 -> 10
            delayedClips.size < 100 -> 20
            delayedClips.size < 150 -> 30
            delayedClips.size < 200 -> 40
            else -> 50
        }
        resultAudio = mergeAudioClips(delayedClips)
            .apply { increaseVolumeOf(this, dB) }
            .apply {
                val oldPath = this.path
                updateAudioInfo(this, RESULT_FILE_NAME)
                File(oldPath!!).renameTo(file)
            }
    }

    fun cutAudio(audio: Audio, cutTime: CutTime) {
        val resultFileName = "cutAudio_" + audio.fileName
        val cmd = listOf(
            FFMPEG_PATH,
            "-i",
            audio.path,
            "-ss",
            cutTime.startTime.toString() + "ms",
            "-t",
            cutTime.endTime.toString() + "ms",
            "-acodec",
            "copy",
            audio.dir + resultFileName,
            "-y"
        )
        val processBuilder = ProcessBuilder(cmd)
        processBuilder
            .redirectOutput(ProcessBuilder.Redirect.DISCARD)
            .redirectErrorStream(true)
        val process = processBuilder.start()
        logger.info("Executing ${cmd.joinToString(separator = " ")}.")

        val status = process.waitFor()
        if (status != 0) {
            logger.warn(
                "Failed to execute ffmpeg command in method ${object {}.javaClass.enclosingMethod.name}. " +
                        "The return code is $status"
            )
        }
        resultAudio.apply {
            val oldPath = this.path
            updateAudioInfo(this, RESULT_FILE_NAME)
            File(oldPath!!).renameTo(file)
        }
        logger.info("Done ${object {}.javaClass.enclosingMethod.name}.")
    }

    private fun delayAudio(audio: Audio): Audio {
        val resultFileName = "${audio.delayTime}_" + audio.fileName
        val cmd = listOf(
            FFMPEG_PATH,
            "-i",
            ORIGIN_AUDIO_DIR + audio.fileName,
            "-filter_complex",
            "adelay=delays=" + audio.delayTime + ":all=1",
            audio.dir + resultFileName,
            "-y"
        )
        val processBuilder = ProcessBuilder(cmd)
        processBuilder
            .redirectOutput(ProcessBuilder.Redirect.DISCARD)
            .redirectErrorStream(true)
        val process = processBuilder.start()
        logger.info("Executing ${cmd.joinToString(separator = " ")}.")

        val status = process.waitFor()
        if (status != 0) {
            logger.warn(
                "Failed to execute ffmpeg command in method ${object {}.javaClass.enclosingMethod.name}. " +
                        "The return code is $status"
            )
        }
        // logger.info("Done ${object {}.javaClass.enclosingMethod.name}.")
        return audio.apply {
            updateAudioInfo(this, resultFileName)
        }
    }

    private fun mergeAudioClips(audios: List<Audio>): Audio {
        val resultFileName = "mergedAudioClips.mp3"
        if (audios.size == 1) {
            val audio = audios[0]
            audio.file!!.copyTo(
                File(audio.dir + resultFileName),
                overwrite = true,
                bufferSize = DEFAULT_BUFFER_SIZE
            )
            return audio.apply {
                updateAudioInfo(this, resultFileName)
            }
        }

        val cmd = mutableListOf(FFMPEG_PATH)
        for (audio in audios) {
            cmd.add("-i")
            audio.path?.let { cmd.add(it) }
        }
        cmd.addAll(
            listOf(
                "-filter_complex",
                "amix=inputs=" + audios.size + ":dropout_transition=300",
                audios[0].dir + resultFileName,
                "-y"
            )
        )

        val processBuilder = ProcessBuilder(cmd)
        processBuilder
            .redirectOutput(ProcessBuilder.Redirect.DISCARD)
            .redirectErrorStream(true)
        val process = processBuilder.start()
        logger.info("Executing $cmd.")
        // printProcess(process.inputStream, System.out)

        val status = process.waitFor()
        if (status != 0) {
            logger.warn(
                "Failed to execute ffmpeg command in method ${object {}.javaClass.enclosingMethod.name}. " +
                        "The return code is $status"
            )
        }
        logger.info("Done ${object {}.javaClass.enclosingMethod.name}.")
        return Audio().apply {
            updateAudioInfo(this, resultFileName)
        }
    }

    private fun increaseVolumeOf(audio: Audio, dB: Int): Audio {
        val resultFileName = "volumeUp_" + audio.fileName
        val cmd = listOf(
            FFMPEG_PATH,
            "-i",
            audio.path,
            "-filter",
            "volume=" + dB + "dB",
            audio.dir + resultFileName,
            "-y"
        )
        val processBuilder = ProcessBuilder(cmd)
        processBuilder
            .redirectOutput(ProcessBuilder.Redirect.DISCARD)
            .redirectErrorStream(true)
        val process = processBuilder.start()
        logger.info("Executing ${cmd.joinToString(separator = " ")}.")

        val status = process.waitFor()
        if (status != 0) {
            logger.warn(
                "Failed to execute ffmpeg command in method ${object {}.javaClass.enclosingMethod.name}. " +
                        "The return code is $status"
            )
        }
        logger.info("Done ${object {}.javaClass.enclosingMethod.name}.")
        return audio.apply {
            updateAudioInfo(this, resultFileName)
        }
    }

    // 音量标准化
    private fun loudnorm(audio: Audio): Audio {
        val resultFileName = "volumeUp_" + audio.fileName
        val cmd = listOf(
            FFMPEG_PATH,
            "-i",
            audio.path,
            "-filter:a",
            "loudnorm",
            audio.dir + audio.fileName,
            "-y"
        )
        val processBuilder = ProcessBuilder(cmd)
        processBuilder
            .redirectOutput(ProcessBuilder.Redirect.DISCARD)
            .redirectErrorStream(true)
        val process = processBuilder.start()
        logger.info("Executing ${cmd.joinToString(separator = " ")}.")

        val status = process.waitFor()
        if (status != 0) {
            logger.warn(
                "Failed to execute ffmpeg command in method ${object {}.javaClass.enclosingMethod.name}. " +
                        "The return code is $status"
            )
        }
        logger.info("Done ${object {}.javaClass.enclosingMethod.name}.")
        return audio.apply {
            updateAudioInfo(this, resultFileName)
        }
    }

    private fun printProcess(inputStream: InputStream, out: PrintStream) {
        try {
            val reader = BufferedReader(InputStreamReader(inputStream))
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                out.println(line)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            try {
                inputStream.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    fun updateAudioInfo(audio: Audio, newFileName: String) {
        audio.fileName = newFileName
        audio.name = audio.fileName!!.substring(0, audio.fileName!!.lastIndexOf("."))
        audio.dir = TMP_WORKING_DIR + id + "/"
        audio.path = audio.dir + audio.fileName
        audio.file = File(audio.path!!)
    }
}