package per.chowhound.bot.mirai.framework.components.music.kugou.service

import per.chowhound.bot.mirai.framework.components.music.kugou.entity.Music

interface MusicService {
    /**
     * 通过歌曲名获取歌曲,模糊查询
     * @return 歌曲列表，每个Music对象的isLocal属性为true
     */
    fun likeMusic(audioName: String): MutableList<Music>
}
