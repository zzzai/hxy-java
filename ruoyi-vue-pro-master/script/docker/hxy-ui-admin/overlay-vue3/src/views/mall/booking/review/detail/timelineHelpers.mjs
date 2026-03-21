const followStatusMap = {
  0: '无需跟进',
  1: '待跟进',
  2: '跟进中',
  3: '已解决',
  4: '已关闭'
};

const timelineDefinitions = [
  {
    key: 'submitTime',
    label: '提交评价',
    description: (review) => `总体评分 ${review.overallScore ?? '-'} · ${review.reviewLevel === 3 ? '差评' : review.reviewLevel === 2 ? '中评' : '好评'}`
  },
  {
    key: 'firstResponseAt',
    label: '首次响应已记录',
    description: () => '系统记录的首次响应时间'
  },
  {
    key: 'replyTime',
    label: '已正式回复用户',
    description: (review) => `回复者 ${review.replyUserId || '未知'}`
  },
  {
    key: 'managerClaimedAt',
    label: '店长待办已认领',
    description: (review) => `认领人 ${review.managerClaimedByUserId || '未记录'}`
  },
  {
    key: 'managerFirstActionAt',
    label: '已记录首次处理',
    description: () => '运营已进入首次处理流程'
  },
  {
    key: 'managerClosedAt',
    label: '店长待办已闭环',
    description: () => '闭环完成'
  }
];

const parseTime = (value) => {
  if (!value) {
    return NaN;
  }
  return new Date(value.replace(/-/g, '/')).getTime();
};

const buildSummaryItems = (review) => {
  const items = [];

  items.push({
    label: '当前跟进状态',
    value: followStatusMap[review.followStatus] ?? '未设定'
  });

  items.push({
    label: '当前回复状态',
    value: review.replyStatus ? '已回复' : '未回复'
  });

  if (review.managerTodoStatus !== undefined && review.managerTodoStatus !== null) {
    items.push({
      label: '店长待办状态',
      value: `状态 ${review.managerTodoStatus}`
    });
  }

  if (review.managerLatestActionRemark) {
    items.push({
      label: '最近处理备注',
      value: review.managerLatestActionRemark
    });
  }

  if (review.followResult) {
    items.push({
      label: '当前跟进结论',
      value: review.followResult
    });
  }

  if (review.replyContent) {
    items.push({
      label: '最新回复摘要',
      value: review.replyContent
    });
  }

  return items;
};

export const buildReviewDetailTimeline = (review = {}) => {
  const timelineItems = [];

  timelineDefinitions.forEach(({ key, label, description }) => {
    if (!review[key]) {
      return;
    }
    const time = parseTime(review[key]);
    if (Number.isNaN(time)) {
      return;
    }
    timelineItems.push({
      label,
      time: review[key],
      timestamp: time,
      description: description(review)
    });
  });

  timelineItems.sort((a, b) => a.timestamp - b.timestamp);

  return {
    summaryItems: buildSummaryItems(review),
    timelineItems
  };
};
