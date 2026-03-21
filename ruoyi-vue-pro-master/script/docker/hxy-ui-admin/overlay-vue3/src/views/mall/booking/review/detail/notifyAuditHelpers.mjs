const CHANNELS = ['IN_APP', 'WECOM'];

const channelMetaMap = {
  IN_APP: {
    channel: 'IN_APP',
    channelLabel: 'App 通道',
    shortLabel: 'App'
  },
  WECOM: {
    channel: 'WECOM',
    channelLabel: '企微通道',
    shortLabel: '企微'
  }
};

const statusTextMap = {
  PENDING: '待派发',
  SENT: '已发送',
  FAILED: '发送失败',
  BLOCKED_NO_OWNER: '路由阻断'
};

const getStatusText = (status) => statusTextMap[status] || '未识别状态';

const createMissingSnapshot = (channel) => {
  const meta = channelMetaMap[channel] || {
    channel,
    channelLabel: channel,
    shortLabel: channel
  };
  return {
    exists: false,
    channel,
    channelLabel: meta.channelLabel,
    shortLabel: meta.shortLabel,
    status: 'MISSING',
    statusText: '未核出当前通道记录',
    receiverLabel: '未核出接收账号',
    diagnosticLabel: '当前未查询到该通道的 notify outbox 记录',
    actionLabel: '未核出动作',
    actionOperatorLabel: '-',
    actionReason: '-',
    repairHint: '先核查该门店是否生成了当前通道的通知记录，再判断是否需要修复路由绑定',
    detail: '当前聚合视图未找到该通道记录，详情仍以原始出站台账为准',
    lastActionTime: '-'
  };
};

const createChannelSnapshot = (outbox, channel) => {
  const meta = channelMetaMap[channel] || {
    channel,
    channelLabel: channel,
    shortLabel: channel
  };
  const receiverPieces = [];
  if (outbox?.receiverRole) {
    receiverPieces.push(outbox.receiverRole);
  }
  if (outbox?.receiverAccount) {
    receiverPieces.push(outbox.receiverAccount);
  } else if (outbox?.receiverUserId !== undefined && outbox?.receiverUserId !== null) {
    receiverPieces.push(`ID:${outbox.receiverUserId}`);
  }
  return {
    exists: true,
    channel,
    channelLabel: meta.channelLabel,
    shortLabel: meta.shortLabel,
    status: outbox?.status || '-',
    statusText: getStatusText(outbox?.status),
    receiverLabel: receiverPieces.join(' / ') || '未核出接收账号',
    diagnosticLabel: outbox?.diagnosticLabel || '未核出诊断结论',
    actionLabel: outbox?.actionLabel || outbox?.lastActionCode || '未记录动作',
    actionOperatorLabel: outbox?.actionOperatorLabel || '-',
    actionReason: outbox?.actionReason || '-',
    repairHint: outbox?.repairHint || '以原始 notify outbox 台账和门店路由真值页为准',
    detail:
      outbox?.diagnosticDetail ||
      outbox?.lastErrorMsg ||
      outbox?.diagnosticLabel ||
      '当前无额外错误详情，详情仍以原始出站台账为准',
    lastActionTime: outbox?.lastActionTime || outbox?.sentTime || outbox?.createTime || '-'
  };
};

const summarizeFromChannels = ({ notifyRiskSummary, channels, metrics }) => {
  if (notifyRiskSummary) {
    return notifyRiskSummary;
  }

  const existingChannels = CHANNELS.map((key) => channels[key]).filter((item) => item?.exists);
  const blocked = existingChannels.filter((item) => item.status === 'BLOCKED_NO_OWNER');
  const failed = existingChannels.filter((item) => item.status === 'FAILED');
  const pending = existingChannels.filter((item) => item.status === 'PENDING');
  const missing = CHANNELS.map((key) => channels[key]).filter((item) => !item?.exists);

  if (blocked.length === 2) {
    return '双通道阻断';
  }
  if (blocked.length === 1) {
    return `${blocked[0].shortLabel}路由阻断`;
  }
  if (failed.length === 2) {
    return '双通道发送失败';
  }
  if (failed.length === 1) {
    return `${failed[0].shortLabel}发送失败`;
  }
  if (pending.length === 2) {
    return '双通道待派发';
  }
  if (metrics.sentCount === 2) {
    return '双通道已发送';
  }
  if (missing.length === 2) {
    return '未核出双通道记录';
  }
  if (missing.length === 1 && existingChannels.length === 1) {
    return `${existingChannels[0].shortLabel}${existingChannels[0].statusText}，${missing[0].shortLabel}未核出`;
  }
  if (existingChannels.length === 1) {
    return `${existingChannels[0].shortLabel}${existingChannels[0].statusText}`;
  }
  return '以原始 notify outbox 台账为准';
};

export const buildNotifyAuditSnapshot = ({ notifyRiskSummary, notifyOutboxList = [] } = {}) => {
  const latestByChannel = {};
  notifyOutboxList.forEach((item) => {
    if (!item?.channel || latestByChannel[item.channel]) {
      return;
    }
    latestByChannel[item.channel] = item;
  });

  const channels = {
    IN_APP: latestByChannel.IN_APP
      ? createChannelSnapshot(latestByChannel.IN_APP, 'IN_APP')
      : createMissingSnapshot('IN_APP'),
    WECOM: latestByChannel.WECOM
      ? createChannelSnapshot(latestByChannel.WECOM, 'WECOM')
      : createMissingSnapshot('WECOM')
  };

  const metrics = {
    sentCount: notifyOutboxList.filter((item) => item?.status === 'SENT').length,
    pendingCount: notifyOutboxList.filter((item) => item?.status === 'PENDING').length,
    failedCount: notifyOutboxList.filter((item) => item?.status === 'FAILED').length,
    blockedCount: notifyOutboxList.filter((item) => item?.status === 'BLOCKED_NO_OWNER').length
  };

  return {
    summary: summarizeFromChannels({ notifyRiskSummary, channels, metrics }),
    metrics,
    channels
  };
};
