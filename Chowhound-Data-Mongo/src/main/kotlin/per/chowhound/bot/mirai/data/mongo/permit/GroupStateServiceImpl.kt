package per.chowhound.bot.mirai.data.mongo.permit

import org.springframework.cache.annotation.CacheConfig
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.CachePut
import org.springframework.cache.annotation.Cacheable
import org.springframework.data.mongodb.repository.DeleteQuery
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.mongodb.repository.Query
import org.springframework.stereotype.Repository
import org.springframework.stereotype.Service
import per.chowhound.bot.mirai.framework.components.permit.entity.Permit
import per.chowhound.bot.mirai.framework.components.permit.enums.PermitEnum
import per.chowhound.bot.mirai.framework.components.permit.service.PermitService

/**
 * @Author: Chowhound
 * @Date: 2023/4/25 - 13:47
 * @Description:
 */
@Suppress("SpringDataMongoDBJsonFieldInspection")
@Repository
interface PermitDetailRepository: MongoRepository<Permit, String> {
    @Query("{qqNumber: ?0}")
    fun findOneByQqNumber(qqNumber: Long): Permit?
    @Query("{permit: ?0}")
    fun findAllByPermit(permit: PermitEnum): List<Permit>

    @DeleteQuery("{qq_number: ?0}")
    fun deleteByQqNumber(qqNumber: Long): List<Permit>

}


@CacheConfig(cacheNames = ["permit"])
@Service
class PermitDetailServiceImpl(val repository: PermitDetailRepository): PermitService {

    @Cacheable(key = "#qqNumber")
    override fun getPermit(qqNumber: Long):Permit {

        return repository.findOneByQqNumber(qqNumber) ?: Permit(qqNumber = qqNumber, permit = PermitEnum.MEMBER)
    }
    @CachePut(key = "#qqNumber")
    override fun setPermit(qqNumber: Long, permit: PermitEnum):Permit {

        return repository.save(Permit(qqNumber = qqNumber, permit = permit))
    }
    @CacheEvict(allEntries = true)
    override fun setUsedHostPermit(qqNumber: Long, permit: PermitEnum): MutableList<Permit> {


        repository.deleteByQqNumber(qqNumber)

        // 将repository.findAllByPermit(PermitEnum.OWNER)返回值每个元素的permit属性设置为permit
        val permitList = repository.findAllByPermit(PermitEnum.OWNER).map { it.permit = permit;it }

        return repository.saveAll(permitList)
    }
}