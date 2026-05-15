package com.banana.toolbox.domain.usecase.tools

import android.graphics.Color
import java.security.SecureRandom
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 各种生成工具用例
 */
@Singleton
class GeneratorUseCases @Inject constructor() {
    
    private val random = SecureRandom()
    
    // ==================== UUID 生成 ====================
    
    /**
     * 生成 UUID
     */
    fun generateUUID(version: UuidVersion = UuidVersion.V4): String {
        return when (version) {
            UuidVersion.V4 -> UUID.randomUUID().toString()
            UuidVersion.V4_NO_DASHES -> UUID.randomUUID().toString().replace("-", "")
            UuidVersion.V4_UPPER -> UUID.randomUUID().toString().uppercase()
            UuidVersion.V3 -> UUID.nameUUIDFromBytes(random.nextBytes(16)).toString()
        }
    }
    
    /**
     * 批量生成 UUID
     */
    fun generateUUIDs(count: Int, version: UuidVersion = UuidVersion.V4): List<String> {
        return (1..count).map { generateUUID(version) }
    }
    
    // ==================== 条形码生成 ====================
    
    /**
     * 生成 Code128 条形码数据
     */
    fun generateBarcodeData(content: String, type: BarcodeType = BarcodeType.CODE128): BarcodeData {
        return when (type) {
            BarcodeType.CODE128 -> BarcodeData(
                content = content,
                type = "CODE128",
                width = content.length * 11, // 近似宽度
                isValid = content.all { it.code in 32..126 }
            )
            BarcodeType.EAN13 -> BarcodeData(
                content = content,
                type = "EAN-13",
                width = 95,
                isValid = content.length == 13 && content.all { it.isDigit() }
            )
            BarcodeType.EAN8 -> BarcodeData(
                content = content,
                type = "EAN-8",
                width = 67,
                isValid = content.length == 8 && content.all { it.isDigit() }
            )
            BarcodeType.UPC_A -> BarcodeData(
                content = content,
                type = "UPC-A",
                width = 95,
                isValid = content.length == 12 && content.all { it.isDigit() }
            )
        }
    }
    
    // ==================== 调色板生成 ====================
    
    /**
     * 从基色生成调色板
     */
    fun generatePalette(baseColor: Int, count: Int = 5): List<PaletteColor> {
        val colors = mutableListOf<PaletteColor>()
        val hsl = FloatArray(3)
        Color.colorToHSV(baseColor, hsl)
        
        for (i in 0 until count) {
            val hue = (hsl[0] + i * (360f / count)) % 360
            val saturation = hsl[1]
            val lightness = hsl[2]
            
            val color = Color.HSVToColor(floatArrayOf(hue, saturation, lightness))
            colors.add(PaletteColor(
                color = color,
                hex = String.format("#%06X", color and 0xFFFFFF),
                hue = hue,
                saturation = saturation,
                lightness = lightness
            ))
        }
        
        return colors
    }
    
    /**
     * 生成随机调色板
     */
    fun generateRandomPalette(count: Int = 5): List<PaletteColor> {
        val baseHue = random.nextFloat() * 360
        return generatePalette(Color.HSVToColor(floatArrayOf(baseHue, 0.7f, 0.5f)), count)
    }
    
    /**
     * 生成渐变色
     */
    fun generateGradient(
        startColor: Int,
        endColor: Int,
        steps: Int = 10
    ): List<GradientStep> {
        val startHsl = FloatArray(3)
        val endHsl = FloatArray(3)
        Color.colorToHSV(startColor, startHsl)
        Color.colorToHSV(endColor, endHsl)
        
        return (0 until steps).map { i ->
            val ratio = i.toFloat() / (steps - 1)
            val h = startHsl[0] + (endHsl[0] - startHsl[0]) * ratio
            val s = startHsl[1] + (endHsl[1] - startHsl[1]) * ratio
            val l = startHsl[2] + (endHsl[2] - startHsl[2]) * ratio
            
            val color = Color.HSVToColor(floatArrayOf(h, s, l))
            GradientStep(
                color = color,
                hex = String.format("#%06X", color and 0xFFFFFF),
                position = ratio
            )
        }
    }
    
    // ==================== 随机数据生成 ====================
    
    /**
     * 生成随机中文名字
     */
    fun generateChineseName(): String {
        val surnames = listOf("赵","钱","孙","李","周","吴","郑","王","冯","陈","褚","卫","蒋","沈","韩","杨","朱","秦","尤","许","何","吕","施","张","孔","曹","严","华","金","魏","陶","姜","戚","谢","邹","喻","柏","水","窦","章","云","苏","潘","葛","奚","范","彭","郎","鲁","韦","昌","马","苗","凤","花","方","俞","任","袁","柳","丰","鲍","史","唐","费","廉","岑","薛","雷","贺","倪","汤","滕","殷","罗","毕","郝","邬","安","常","乐","于","时","傅","皮","卞","齐","康","伍","余","元","卜","顾","孟","平","黄","和","穆","萧","尹","姚","邵","湛","汪","祁","毛","禹","狄","米","贝","明","臧","计","伏","成","戴","谈","宋","茅","庞","熊","纪","舒","屈","项","祝","董","梁","杜","阮","蓝","闵","席","季","麻","强","贾","路","娄","危","江","童","颜","郭","梅","盛","林","刁","钟","徐","邱","骆","高","夏","蔡","田","樊","胡","凌","霍","虞","万","支","柯","昝","管","卢","莫","经","房","裘","缪","干","解","应","宗","丁","宣","贲","邓","郁","单","杭","洪","包","诸","左","石","崔","吉","钮","龚","程","嵇","邢","滑","裴","陆","荣","翁","荀","羊","於","惠","甄","曲","家","封","芮","羿","储","靳","汲","邴","糜","松","井","段","富","巫","乌","焦","巴","弓","牧","隗","山","谷","车","侯","宓","蓬","全","郗","班","仰","秋","仲","伊","宫","宁","仇","栾","暴","甘","钭","厉","戎","祖","武","符","刘","景","詹","束","龙","叶","幸","司","韶","郜","黎","蓟","溥","印","宿","白","怀","蒲","邰","从","鄂","索","咸","籍","赖","卓","蔺","屠","蒙","池","乔","阴","郁","胥","能","苍","双","闻","莘","党","翟","谭","贡","劳","逄","姬","申","扶","堵","冉","宰","郦","雍","却","璩","桑","桂","濮","牛","寿","通","边","扈","燕","冀","浦","尚","农","温","别","庄","晏","柴","瞿","阎","充","慕","连","茹","习","宦","艾","鱼","容","向","古","易","慎","戈","廖","庾","终","暨","居","衡","步","都","耿","满","弘","匡","国","文","寇","广","禄","阙","东","欧","殳","沃","利","蔚","越","夔","隆","师","巩","厍","聂","晁","勾","敖","融","冷","訾","辛","阚","那","简","饶","空","曾","毋","沙","乜","养","鞠","须","丰","巢","关","蒯","相","查","后","荆","红","游","竺","权","逯","盖","益","桓","公","万俟","司","马","上","官","欧阳","夏","侯","诸葛","闻","人","东方","赫连","皇","甫","尉","迟","公","羊","澹","台","公","冶","宗","政","濮","阳","淳","于","单","于","太","叔","申","屠","公孙","仲","孙","轩","辕","令","狐","钟","离","宇","文","长","孙","慕","容","鲜","于","闾","丘","司","徒","司","空","亓","官","司","寇","仉","督","子","车","颛","孙","端","木","巫","马","公","西","漆","雕","乐","正","壤","驷","良","拓","跋","夹","谷","宰","父","谷","梁","晋","楚","阎","法","汝","鄢","涂","钦","段","干","百里","东","郭","南","门","呼","延","归","海","羊","舌","微","生","岳","帅","缑","亢","况","后","有","琴","梁","丘","左","丘","东","门","西","门","商","牟","佘","佴","伯","赏","南","宫","墨","哈，谯，笪，年，爱，阳，佟")
                        
        val givenNames = listOf("伟","芳","娜","秀英","敏","静","丽","强","磊","军","洋","勇","艳","杰","娟","涛","明","超","秀兰","霞","平","刚","桂英","文","云","华","慧","建华","玲","建国","建军","峰","浩","小红","志强","丹","萍","鹏","辉","嘉","彬","宇","欣","博","铭","昊","天","翔","雨","泽","晨","阳","旭","冰","洁","雪","怡","婷","悦","可","馨","蕊","瑶","琳","萱","妍","彤","菲","寒","烟","月","星","辰","风","雷","龙","虎","鹰","鹤","松","竹","梅","兰","菊","荷","莲","薇","蓝","青","紫","墨","白","玄","玉","珠","翠","瑶","璃","瑶","琪","瑛","珊","珍","璐","黛","绮","绮","倩","婧","婕","妤","媛","嫣","柔","颖","雯","霏","黛")
                        
                        return "${surnames.random()}${givenNames.random()}"
                    }
                    
                    /**
                     * 生成随机手机号
                     */
                    fun generatePhoneNumber(prefix: String = "1"): String {
                        val prefixes = when (prefix) {
                            "移动" -> listOf("134", "135", "136", "137", "138", "139", "147", "150", "151", "152", "157", "158", "159", "178", "182", "183", "184", "187", "188", "198")
                            "联通" -> listOf("130", "131", "132", "145", "155", "156", "166", "167", "171", "175", "176", "185", "186")
                            "电信" -> listOf("133", "149", "153", "173", "174", "177", "180", "181", "189", "191", "193", "199")
                            else -> listOf("130","131","132","133","134","135","136","137","138","139","147","148","149","150","151","152","153","155","156","157","158","159","166","167","170","171","172","173","174","175","176","177","178","180","181","182","183","184","185","186","187","188","189","191","193","198","199")
                        }
                        
                        val pre = prefixes.random()
                        val suffix = (1..8).map { random.nextInt(10) }.joinToString("")
                        return "$pre$suffix"
                    }
                    
                    /**
                     * 生成随机邮箱
                     */
                    fun generateEmail(domain: String? = null): String {
                        val domains = domain?.let { listOf(it) } ?: listOf(
                            "gmail.com", "outlook.com", "qq.com", "163.com", "126.com",
                            "yahoo.com", "hotmail.com", "icloud.com", "proton.me"
                        )
                        val usernames = listOf(
                            "user", "admin", "test", "hello", "cool", "star", "moon",
                            "sun", "rain", "snow", "wind", "fire", "water", "earth",
                            "sky", "sea", "lake", "river", "mountain", "forest"
                        )
                        val suffix = random.nextInt(9999)
                        return "${usernames.random()}$suffix@${domains.random()}"
                    }
                    
                    /**
                     * 生成随机 IP 地址
                     */
                    fun generateIpAddress(privateIp: Boolean = true): String {
                        return if (privateIp) {
                            val ranges = listOf(
                                Triple(10, 0, 255),
                                Triple(172, 16, 31),
                                Triple(192, 168, 0)
                            )
                            val range = ranges.random()
                            "${range.first}.${range.second + random.nextInt(range.third - range.second + 1)}.${random.nextInt(256)}.${random.nextInt(256)}"
                        } else {
                            "${random.nextInt(223) + 1}.${random.nextInt(256)}.${random.nextInt(256)}.${random.nextInt(256)}"
                        }
                    }
                    
                    /**
                     * 生成随机 MAC 地址
                     */
                    fun generateMacAddress(): String {
                        return (1..6).map {
                            "%02X".format(random.nextInt(256))
                        }.joinToString(":")
                    }
                    
                    /**
                     * 生成随机 URL
                     */
                    fun generateUrl(): String {
                        val protocols = listOf("https", "http")
                        val domains = listOf("example.com", "test.org", "demo.net", "sample.io", "mock.dev")
                        val paths = listOf("api/v1/users", "page/home", "data/list", "content/detail", "search", "login", "register")
                        return "${protocols.random()}://${domains.random()}/${paths.random()}"
                    }
                    
                    /**
                     * 生成随机日期
                     */
                    fun generateDate(format: String = "yyyy-MM-dd"): String {
                        val calendar = Calendar.getInstance()
                        calendar.timeInMillis = System.currentTimeMillis() - random.nextLong() * 365L * 24 * 60 * 60 * 1000
                        return SimpleDateFormat(format, Locale.getDefault()).format(calendar.time)
                    }
                    
                    /**
                     * 生成随机地址
                     */
                    fun generateAddress(): String {
                        val provinces = listOf("北京市","上海市","广东省广州市","浙江省杭州市","江苏省南京市","四川省成都市","湖北省武汉市","湖南省长沙市","山东省济南市","河南省郑州市","福建省福州市","安徽省合肥市","河北省石家庄市","陕西省西安市","辽宁省沈阳市","吉林省长春市","黑龙江省哈尔滨市","重庆市","天津市")
                        val streets = listOf("中山路","人民路","建设路","解放路","和平路","新华路","长江路","黄河路","文化路","科技路","创新路","幸福路","光明路","友谊路","团结路")
                        return "${provinces.random()}${streets.random()}${random.nextInt(200) + 1}号"
                    }
                    
                    /**
                     * 生成 Lorem Ipsum 文本
                     */
                    fun generateLoremIpsum(wordCount: Int = 50): String {
                        val words = "Lorem ipsum dolor sit amet consectetur adipiscing elit sed do eiusmod tempor incididunt ut labore et dolore magna aliqua Ut enim ad minim veniam quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur Excepteur sint occaecat cupidatat non proident sunt in culpa qui officia deserunt mollit anim id est laborum".split(" ")
                        
                        return (1..wordCount).map { words.random() }.joinToString(" ") + "."
                    }
                    
                    /**
                     * 生成随机中文文本
                     */
                    fun generateChineseText(charCount: Int = 100): String {
                        val chars = "天地玄黄宇宙洪荒日月盈昃辰宿列张寒来暑往秋收冬藏闰余成岁律吕调阳云腾致雨露结为霜金生丽水玉出昆冈剑号巨珠称夜光果珍李柰菜重芥姜海咸河淡鳞潜羽翔龙师火帝鸟官人皇始制文字乃服衣裳推位让国有虞陶唐吊民伐罪周发殷汤坐朝问道垂拱平章爱育黎首臣伏戎羌遐迩一体率宾归王鸣凤在竹白驹食场化被草木赖及万方盖此身发四大五常恭惟鞠养岂敢毁伤女慕贞洁男效才良知过必改得能莫忘罔谈彼短靡恃己长信使可覆器欲难量墨悲丝染诗赞羔羊景行维贤克念作圣德建名立形端表正空谷传声虚堂习听祸因恶积福缘善庆尺璧非宝寸阴是竞资父事君曰严与敬孝当竭力忠则尽命临深履薄夙兴温凊似兰斯馨如松之盛川流不息渊澄取映容止若思言辞安定笃初诚美慎终宜令荣业所基籍甚无竟学优登仕摄职从政存以甘棠去而益咏乐殊贵贱礼别尊卑上和下睦夫唱妇随外受傅训入奉母仪诸姑伯叔犹子比儿孔怀兄弟同气连枝交友投分切磨箴规仁慈隐恻造次弗离节义廉退颠沛匪亏性静情逸心动神疲守真志满逐物意移坚持雅操好爵自縻都邑华夏东西二京背邙面洛浮渭据泾宫殿盘郁楼观飞惊图写禽兽画彩仙灵丙舍傍启甲帐对楹肆筵设席鼓瑟吹笙升阶纳陛弁转疑星右通广内左达承明既集坟典亦聚群英杜稿钟隶漆书壁经府罗将将路侠槐卿户封八县家给千兵高冠陪辇驱毂振缨世禄侈富车驾肥轻策功茂实勒碑刻铭磻溪伊尹佐时阿衡奄宅曲阜微旦孰营桓公匡合济弱扶倾绮回汉惠说感武丁俊乂密勿多宁晋楚更霸赵魏困横假途灭虢践土会盟何遵约法韩弊烦刑起翦颇牧用军最宣威沙漠驰誉丹青九州禹迹百郡秦并岳宗泰岱禅主云亭雁门紫塞鸡田赤城昆池碣石巨野洞庭旷远绵邈岩岫杳冥治本于农务兹稼穑俶载南亩我艺黍稷税熟贡新劝赏黜陟孟轲敦素史鱼秉直庶几中庸劳谦谨敕聆音察理鉴貌辨色贻厥嘉猷勉其祗植省躬讥诫宠增抗极殆辱近耻林皋幸即源两疏见机解组谁逼索居闲处沉默寂寥求古寻论散虑逍遥欣奏累遣戚谢欢招渠荷的历园莽抽条枇杷梧桐早凋陈根委翳落叶飘摇游鹍独运凌摩绛霄耽读玩市寓目囊箱易輶攸畏属耳垣墙具膳餐饭适口充肠饱饫烹宰节再再".toCharArray()
                        
                        return (1..charCount).map { chars.random() }.joinToString("")
                    }
                    
                    /**
                     * 生成随机数
                     */
                    fun generateRandomNumber(min: Int, max: Int): Int {
                        return random.nextInt(max - min + 1) + min
                    }
                    
                    /**
                     * 生成随机浮点数
                     */
                    fun generateRandomFloat(min: Double, max: Double, decimals: Int = 2): Double {
                        val value = min + random.nextDouble() * (max - min)
                        return "%.${decimals}f".format(value).toDouble()
                    }
                    
                    /**
                     * 生成随机十六进制字符串
                     */
                    fun generateHexString(length: Int): String {
                        return (1..length).map { "%X".format(random.nextInt(16)) }.joinToString("")
                    }
                    
                    /**
                     * 生成随机 Base64 字符串
                     */
                    fun generateBase64(length: Int): String {
                        val bytes = ByteArray(length)
                        random.nextBytes(bytes)
                        return Base64.getEncoder().encodeToString(bytes)
                    }
                }
                
                enum class UuidVersion { V4, V4_NO_DASHES, V4_UPPER, V3 }
                enum class BarcodeType { CODE128, EAN13, EAN8, UPC_A }
                
                data class BarcodeData(
                    val content: String,
                    val type: String,
                    val width: Int,
                    val isValid: Boolean
                )
                
                data class PaletteColor(
                    val color: Int,
                    val hex: String,
                    val hue: Float,
                    val saturation: Float,
                    val lightness: Float
                )
                
                data class GradientStep(
                    val color: Int,
                    val hex: String,
                    val position: Float
                )
