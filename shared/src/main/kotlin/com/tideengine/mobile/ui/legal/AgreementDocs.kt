package com.tideengine.mobile.ui.legal

/**
 * 用户协议 / 隐私政策文案：与 Web 端 client/src/pages/AgreementsPage.tsx 逐字一致
 * （服务端行为变化时两边必须同步更新）。App 内置展示，不做网页跳转。
 *
 * 结构化存储（段落 / 列表块），由 DocumentSheet 渲染，保持与 Web 相同的文档层次。
 */

/** 文档内容块：段落或项目符号列表，保持渲染结构简单可控。 */
sealed interface AgreementBlock {
    data class Paragraph(val text: String) : AgreementBlock
    data class Bullets(val items: List<String>) : AgreementBlock
}

data class AgreementSection(val title: String, val blocks: List<AgreementBlock>)

data class AgreementDoc(
    val title: String,
    val version: String,
    val effectiveDate: String,
    val intro: String,
    val sections: List<AgreementSection>,
)

val USER_AGREEMENT: AgreementDoc = AgreementDoc(
    title = "用户协议",
    version = "v1.0",
    effectiveDate = "2026 年 11 月 22 日",
    intro = "欢迎使用潮汐引擎（以下简称\"本服务\"）。本协议是您与潮汐引擎运营方（以下简称\"我们\"）之间关于使用本服务所订立的协议。请您在使用前仔细阅读本协议，重点条款已加粗提示，您确认或继续使用即视为已阅读并同意本协议全部内容。",
    sections = listOf(
        AgreementSection(
            title = "一、协议的接受与修改",
            blocks = listOf(
                AgreementBlock.Paragraph(
                    "您通过页面勾选确认、完成注册或以任何方式实际使用本服务，均视为您已充分理解并接受本协议的全部条款。若您不同意本协议的任何内容，请立即停止注册或使用本服务。",
                ),
                AgreementBlock.Paragraph(
                    "我们可能根据服务调整与法律法规变化对本协议进行修订，修订后的协议将通过页面公告或其他合理方式通知您。若您在协议修订后继续使用本服务，视为您接受修订后的协议；若不接受，请停止使用并可联系管理员注销账号。",
                ),
            ),
        ),
        AgreementSection(
            title = "二、服务说明",
            blocks = listOf(
                AgreementBlock.Paragraph("本服务面向个人用户提供游戏账号的统一管理与自动化辅助能力，当前主要包括："),
                AgreementBlock.Bullets(
                    listOf(
                        "绑定与管理多个游戏账号，并指定默认账号；",
                        "创建与调度自动任务（如答题、领奖等），查看执行结果与操作日志；",
                        "查看账号状态概览与执行记录；",
                        "账户资料、密码与登录方式管理，以及余额充值。",
                    ),
                ),
                AgreementBlock.Paragraph("我们持续改进服务形态，个别功能可能随版本调整而新增、变化或下线，恕不另行逐一通知，但会在页面或文档中说明。"),
            ),
        ),
        AgreementSection(
            title = "三、账号注册与安全",
            blocks = listOf(
                AgreementBlock.Paragraph(
                    "您应使用本人有效邮箱完成注册，并按页面要求设置用户名、显示名称与密码。自助注册的账号默认为只读角色，更高权限需由管理员审核开通。您应妥善保管账号与密码，不得出借、出租、转让或与他人共用账号。",
                ),
                AgreementBlock.Paragraph(
                    "因您主动泄露密码、遭受他人劫持或保管不当造成的损失，由您自行承担。若发现账号被盗用或异常登录，请立即修改密码并联系管理员处理。",
                ),
            ),
        ),
        AgreementSection(
            title = "四、用户行为规范",
            blocks = listOf(
                AgreementBlock.Paragraph("您承诺在使用本服务时遵守法律法规及以下规范："),
                AgreementBlock.Bullets(
                    listOf(
                        "仅绑定和管理您本人拥有或经合法授权使用的游戏账号；",
                        "不利用本服务从事任何违法、侵权或损害他人合法权益的活动；",
                        "不对本服务进行逆向工程、漏洞攻击、接口滥用、爬取或干扰其正常运行；",
                        "不将本服务用于商业转售、多级分销或其他未经我们书面许可的用途。",
                    ),
                ),
                AgreementBlock.Paragraph("如违反上述规范，我们有权视情节采取警示、限制功能、暂停或终止服务等措施，并保留追究责任的权利。"),
            ),
        ),
        AgreementSection(
            title = "五、游戏账号绑定与自动任务风险提示",
            blocks = listOf(
                AgreementBlock.Paragraph(
                    "您绑定的游戏账号必须为本人所有或已获得账号所有人的明确授权。绑定过程中上传或保存的登录凭据仅用于执行您配置的自动化操作，我们将其加密存储并限制访问范围（详见《隐私政策》）。",
                ),
                AgreementBlock.Paragraph(
                    "自动任务依赖第三方游戏服务的接口与协议。游戏版本更新、接口调整、风控策略变化或网络异常，都可能导致任务失败、延迟或中断。您理解并同意：因第三方游戏服务方策略（包括但不限于账号限制、处罚）所产生的后果由您自行评估并承担，我们不对游戏账号的安全性、可用性或游戏内收益作任何承诺。",
                ),
            ),
        ),
        AgreementSection(
            title = "六、充值与付费",
            blocks = listOf(
                AgreementBlock.Paragraph(
                    "如您通过本服务进行充值，应通过我们提供的正规渠道完成支付。充值余额仅限在本服务内使用，不得转让或提现。已完成消费的金额不予退还；未发生消费的余额如有争议，可联系管理员协商处理。",
                ),
            ),
        ),
        AgreementSection(
            title = "七、知识产权",
            blocks = listOf(
                AgreementBlock.Paragraph(
                    "本服务的软件代码、界面设计、标识与相关文档的知识产权归我们或相应权利人所有。未经许可，任何人不得复制、传播或用于本服务之外的用途。您在使用过程中产生的任务配置与执行记录，归您所有，但我们为提供服务所必需时可对其进行处理与存储。",
                ),
            ),
        ),
        AgreementSection(
            title = "八、免责声明与责任限制",
            blocks = listOf(
                AgreementBlock.Paragraph(
                    "本服务按\"现状\"提供。在法律允许的最大范围内，我们不对以下事项作出保证：服务不间断、无错误；第三方游戏服务的兼容性与稳定性；因不可抗力、网络故障、第三方服务变更导致的服务中断或数据异常。",
                ),
                AgreementBlock.Paragraph(
                    "对于因使用或无法使用本服务导致的间接损失、预期收益损失或第三方游戏账号受限等后果，我们在法律允许的范围内不承担责任。在任何情形下，我们承担的累计赔偿责任不超过您在索赔发生前 12 个月内实际支付的服务费用（如有）。",
                ),
            ),
        ),
        AgreementSection(
            title = "九、服务的变更、中断与终止",
            blocks = listOf(
                AgreementBlock.Paragraph(
                    "我们可因系统维护、升级或不可抗力等原因暂停服务，并将尽可能提前公告。若您长期未使用账号、违反本协议或相关规则，我们有权中止或终止向您提供服务，并依法处理相关数据。",
                ),
            ),
        ),
        AgreementSection(
            title = "十、法律适用与争议解决",
            blocks = listOf(
                AgreementBlock.Paragraph(
                    "本协议的订立、履行与解释适用中华人民共和国法律（不含冲突法规则）。因本协议产生的争议，双方应先友好协商解决；协商不成的，任何一方可向被告住所地有管辖权的人民法院提起诉讼。",
                ),
            ),
        ),
        AgreementSection(
            title = "十一、联系我们",
            blocks = listOf(
                AgreementBlock.Paragraph("如对本协议有任何疑问、意见或建议，您可通过系统内的账户设置或注册邮箱联系管理员，我们将尽快答复。"),
            ),
        ),
    ),
)

val PRIVACY_POLICY: AgreementDoc = AgreementDoc(
    title = "隐私政策",
    version = "v1.0",
    effectiveDate = "2026 年 11 月 22 日",
    intro = "潮汐引擎运营方（以下简称\"我们\"）深知个人信息对您的重要性，我们将按照本政策收集、使用与保护您的信息。请在使用本服务前仔细阅读并充分理解本政策，重点是您的游戏凭据如何被加密存储与使用。",
    sections = listOf(
        AgreementSection(
            title = "一、我们收集的信息",
            blocks = listOf(
                AgreementBlock.Paragraph("为提供本服务，我们会收集以下信息："),
                AgreementBlock.Bullets(
                    listOf(
                        "注册与账户信息：用户名、显示名称、邮箱、密码（仅以不可逆哈希存储，我们无法看到原文）与账户角色；",
                        "游戏账号信息：您主动绑定的游戏角色标识、昵称、区服等展示信息；",
                        "游戏凭据：为登录游戏并执行自动任务所必需的凭据（如登录态、授权信息、原始 Bin 等），仅用于您配置的自动化操作；",
                        "操作与日志信息：任务的创建、启停与执行结果（如执行时间、耗时、领取结果、失败原因摘要）会自动记录为操作日志；",
                        "安全信息：登录会话、登录时间、来源地址与请求频率数据，用于身份识别、限流与安全审计。",
                    ),
                ),
                AgreementBlock.Paragraph("上述信息中，游戏角色标识、凭据原文、完整请求体等敏感内容不会出现在对外的日志或播报中。"),
            ),
        ),
        AgreementSection(
            title = "二、我们如何使用信息",
            blocks = listOf(
                AgreementBlock.Bullets(
                    listOf(
                        "创建与校验账号，维持您的登录状态；",
                        "按您的配置执行游戏自动任务，并生成可查询的执行记录与操作日志；",
                        "在服务重启后恢复加密凭据，保持任务连续性；",
                        "进行限流、异常检测与安全审计，保障账号与服务安全；",
                        "评估服务运行状况并改进产品体验。",
                    ),
                ),
                AgreementBlock.Paragraph("我们不会将您的个人信息用于上述目的之外的用途；如需变更用途，将再次征得您的同意。"),
            ),
        ),
        AgreementSection(
            title = "三、信息的存储与安全保护",
            blocks = listOf(
                AgreementBlock.Paragraph(
                    "游戏凭据（含登录态与原始 Bin）一律以 AES-256-GCM 加密后存储，加密密钥与服务数据分离保管；密码仅以 bcrypt 哈希形式保存。数据传输使用 HTTPS 加密通道。",
                ),
                AgreementBlock.Paragraph(
                    "服务内部按最小必要原则访问数据：普通用户仅能查看自己的信息；管理能力仅授予管理员角色，且相关操作会留痕。系统日志与错误信息经过脱敏处理，不包含凭据原文、令牌或完整请求体。",
                ),
                AgreementBlock.Paragraph("如发生个人信息安全事件，我们将依法及时告知您并采取合理的补救措施。"),
            ),
        ),
        AgreementSection(
            title = "四、信息的共享",
            blocks = listOf(
                AgreementBlock.Paragraph("我们不会向任何第三方出售您的个人信息。除以下情形外，我们不对外共享您的信息："),
                AgreementBlock.Bullets(
                    listOf(
                        "为完成您发起的游戏操作，与对应游戏服务方交互（仅传输执行该操作所必需的内容）；",
                        "根据法律法规、司法机关或行政机关的强制性要求；",
                        "在获得您明确同意的情况下。",
                    ),
                ),
            ),
        ),
        AgreementSection(
            title = "五、信息的留存与删除",
            blocks = listOf(
                AgreementBlock.Paragraph(
                    "游戏凭据默认保留 30 天，到期自动失效；失效后需重新登录游戏方可继续执行自动任务。您解绑游戏账号时，对应的加密凭据与相关数据将被同步删除；注销账户时，我们将删除或匿名化您的个人信息，法律法规另有要求的除外。",
                ),
                AgreementBlock.Paragraph("操作日志用于执行记录追溯，按服务运行需要留存；超出留存目的所需的期限后，我们将进行清理或匿名化处理。"),
            ),
        ),
        AgreementSection(
            title = "六、您的权利",
            blocks = listOf(
                AgreementBlock.Bullets(
                    listOf(
                        "查看与更正：在账户设置中查看、修改您的资料与密码；",
                        "删除与撤回授权：解绑游戏账号即视为撤回对应凭据的使用授权，相关数据同步删除；",
                        "注销账户：联系管理员完成注销，注销后我们将停止使用并删除您的个人信息。",
                    ),
                ),
                AgreementBlock.Paragraph("行使上述权利过程中如有障碍，可通过系统内的账户设置或注册邮箱联系管理员。"),
            ),
        ),
        AgreementSection(
            title = "七、本地存储",
            blocks = listOf(
                AgreementBlock.Paragraph(
                    "为保持登录状态与偏好设置，我们会在您的浏览器本地存储中保存登录令牌与界面主题偏好。这些数据仅保存在您的设备上，清除浏览器数据或退出登录即可删除。",
                ),
            ),
        ),
        AgreementSection(
            title = "八、未成年人保护",
            blocks = listOf(
                AgreementBlock.Paragraph(
                    "本服务面向成年人提供。未满 18 周岁的未成年人使用本服务，须在监护人陪同与同意下进行；我们一经发现未按上述要求使用的未成年人账号，有权中止服务并删除相关信息。",
                ),
            ),
        ),
        AgreementSection(
            title = "九、政策的更新",
            blocks = listOf(
                AgreementBlock.Paragraph(
                    "我们可能适时修订本政策，修订后的政策将在页面发布并注明生效日期。若变更会实质影响您的权益，我们将以更显著的方式提示您。",
                ),
            ),
        ),
        AgreementSection(
            title = "十、联系我们",
            blocks = listOf(
                AgreementBlock.Paragraph("如对本政策有任何疑问、意见或建议，您可通过系统内的账户设置或注册邮箱联系管理员，我们将在合理期限内答复。"),
            ),
        ),
    ),
)
