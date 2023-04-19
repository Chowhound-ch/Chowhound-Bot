package per.chowhound.bot.mirai.data.mongo

import org.springframework.cache.annotation.CacheConfig
import org.springframework.cache.annotation.CachePut
import org.springframework.cache.annotation.Cacheable
import org.springframework.data.domain.Example
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.mongodb.repository.Query
import org.springframework.stereotype.Repository
import org.springframework.stereotype.Service
import per.chowhound.bot.mirai.framework.components.state.entity.GroupState
import per.chowhound.bot.mirai.framework.components.state.service.GroupStateService

/**
 * @Author: Chowhound
 * @Date: 2023/4/19 - 22:07
 * @Description:
 */
@Suppress("SpringDataMongoDBJsonFieldInspection")
@Repository
interface GroupStateRepository: MongoRepository<GroupState, String>{

    // 抑制@Query中value无法解析的警告
    @Query("{group: ?0}")
    fun findOneByGroupNumber(groupNumber: Long): GroupState?
}

@CacheConfig(cacheNames = ["state"])
@Service
class GroupStateServiceImpl(
    val repository: GroupStateRepository
): GroupStateService {

    @Cacheable(key = "#group")
    override fun getGroupState(group: Long): GroupState {

        return  repository.findOneByGroupNumber(group) ?: GroupState(group = group)
    }

    /**
     * true,状态改变, false,状态未改变
     */
    @CachePut(key = "#groupState.group")
    override fun setGroupState(groupState: GroupState): GroupState {
        if (getGroupState(groupState.group!!) == groupState) {
            return groupState
        }

        return groupState.apply {
            repository.save(this)
//            saveOrUpdate(this, KtQueryWrapper(GroupState::class.java).eq(GroupState::groupNumber, groupState.groupNumber))
        }
    }

    override fun getGroupStateByState(state: Int): List<GroupState> {


        return repository.findAll(
            Example.of(GroupState(state = state))
        )
    }

    override fun list(): List<GroupState> {
        return repository.findAll()
    }
}