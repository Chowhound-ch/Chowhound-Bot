package per.chowhound.bot.mirai.data.mongo.music.kugou

import org.springframework.data.domain.Example
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository
import org.springframework.stereotype.Service
import per.chowhound.bot.mirai.framework.components.music.kugou.entity.Music
import per.chowhound.bot.mirai.framework.components.music.kugou.service.MusicService

/**
 * @Author: Chowhound
 * @Date: 2023/4/25 - 20:25
 * @Description:
 */

@Suppress("SpringDataMongoDBJsonFieldInspection")
@Repository
interface MusicRepository: MongoRepository<Music, String>


@Service
class MusicServiceImpl(val musicRepository: MusicRepository): MusicService {
    override fun likeMusic(audioName: String): MutableList<Music> {
        return musicRepository.findAll(Example.of(Music().apply { this.audioNameIndex = audioName }))
            .stream()
            .peek {
                it.isLocal = true
            }.toList()
            .toMutableList()
    }

}