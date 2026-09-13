package com.campusmarket.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.campusmarket.entity.AiConversation;
import com.campusmarket.vo.ConversationVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface AiConversationMapper extends BaseMapper<AiConversation> {

    @Select("""
            SELECT c.id,
                   c.title,
                   (SELECT m.content
                    FROM ai_message m
                    WHERE m.conversation_id = c.id
                    ORDER BY m.create_time DESC, m.id DESC
                    LIMIT 1) AS lastMessage,
                   (SELECT COUNT(*)
                    FROM ai_message m
                    WHERE m.conversation_id = c.id) AS messageCount,
                   c.create_time AS createTime,
                   c.update_time AS updateTime
            FROM ai_conversation c
            WHERE c.user_id = #{userId}
            ORDER BY c.update_time DESC, c.id DESC
            """)
    IPage<ConversationVO> selectConversationPage(IPage<ConversationVO> page,
                                                  @Param("userId") Long userId);
}
