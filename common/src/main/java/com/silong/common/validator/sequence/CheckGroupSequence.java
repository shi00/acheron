package com.silong.common.validator.sequence;

import com.silong.common.validator.group.First;
import com.silong.common.validator.group.Fourth;
import com.silong.common.validator.group.Second;
import com.silong.common.validator.group.Third;
import javax.validation.GroupSequence;
import javax.validation.groups.Default;

/**
 * 参数校验顺序
 *
 * @author sin
 * @version 1.0
 * @since 20170528
 */
@GroupSequence({First.class, Second.class, Third.class, Fourth.class, Default.class})
public interface CheckGroupSequence {

}
