---
title: hexo搭建个人博客
date: 2023-11-11 15:10:04
tags:
---

## 1. 环境准备

### 1.1 安装Git

到[Git 官网](https://git-scm.com/)上下载安装。

安装好后，使用命令查看一下git版本，检查是否安装成功。

```powershell
git -v
```

windows安装完git后，可以使用git bash敲命令。

### 1.2 安装node.js

直接到[官网](https://nodejs.org/en/)上下载。

同样，安装完成后，使用命令查看版本，检查是否安装成功。

```powershell
node -v
npm -v
```

## 2. hexo安装与使用

### 2.1 安装hexo

创建一个文件夹blog，进入该文件夹，右键git bash here打开git bash，输入命令安装hexo：

```powershell
npm install -g hexo-cli
```

### 2.2 初始化hexo

使用以下命令初始化hexo，其中myblog是自己随意取得名字

```powershell
hexo init myblog
```

### 2.3 [安装npm](https://so.csdn.net/so/search?q=安装npm&spm=1001.2101.3001.7020)

进入myblog文件夹，安装npm：

```powershell
cd myblog
npm install
```

新建完成后，指定文件夹目录下有：

- node_modules: 依赖包
- public：存放生成的页面
- scaffolds：生成文章的一些模板
- source：用来存放你的文章
- themes：主题
- _config.yml: 博客的配置文件

### 2.4 启动本地服务站点：

使用如下命令启动本地服务站点，每次提交新的部署之前也可以先启动本地服务站点检查。

```powershell
hexo g //生成静态网页
hexo s //打开本地服务站点
```

在浏览器输入localhost:4000就可以看到生成的博客了，里面默认会有hello world这篇文章。

使用ctrl+c可以关掉本地服务。

hexo是一款静态框架，即我们在本地编写完文章后使用`hexo g`生成静态网页，然后将之部署到服务器上。

下面部署到github page上，以便大家可以访问。

## 3. Github建站访问

### 3.1 Github新建仓库

如果没有Github账号，注册一个，登录。

创建一个和你用户名相同的仓库，后面加.github.io。

![在这里插入图片描述](https://img-blog.csdnimg.cn/05426969336b490bb44ee9e0c4cca386.png#pic_center)

注意：格式必须是xxx.github.io，其中xxx是你注册Github的用户名，只有这样部署到GitHub page的时候，才会被识别。

### 3.2 生成SSH添加到GitHub

回到git bash，输入以下命令：

```powershell
git config --global user.name yourname
git config --global user.email youremail
```

这里的yourname输入你的GitHub用户名，youremail输入你GitHub的邮箱。这样GitHub才能知道你是不是对应它的账户。

可以用以下两条命令，检查一下你有没有输对：

```powershell
git config user.name
git config user.email
```

然后创建SSH，一路回车

```powershell
ssh-keygen -t rsa -C "youremail"
```

这个时候它会告诉你已经生成了.ssh的文件夹。在你的电脑中找到这个文件夹。

![在这里插入图片描述](https://img-blog.csdnimg.cn/d11895a259284724ae495a4da65abcc2.png#pic_center)

而后在GitHub的setting中，找到SSH keys的设置选项，点击`New SSH key`把你的`id_rsa.pub`里面的信息复制进去。

![在这里插入图片描述](https://img-blog.csdnimg.cn/43352f70390245e08fd6684a3215c20d.png#pic_center)

![在这里插入图片描述](https://img-blog.csdnimg.cn/e8bfefd2764d4b1b8cbdfca04ef06907.png#pic_center)

Title随便写个名字，把`id_rsa.pub`里面的信息复制到key那里，点击`Add SSH key`。

在git bash中，使用以下命令查看是否成功：

```powershell
ssh -T git@github.com
```

![在这里插入图片描述](https://img-blog.csdnimg.cn/e49a6788fe0a4af898cb4cefa203ff06.png#pic_center)

### 3.3 将hexo部署到GitHub

这一步，我们就可以将hexo和GitHub关联起来，也就是将hexo生成的文章部署到GitHub上，打开站点配置文件`_config.yml`，翻到最后，修改为如下所示，其中YourgithubName就是你的GitHub账户。

```powershell
deploy:
  type: git
  repo: https://github.com/YourgithubName/YourgithubName.github.io.git
  branch: master
```

这个时候需要先安装deploy-git ，也就是部署的命令,这样你才能用命令部署到GitHub。

```powershell
npm install hexo-deployer-git --save
```

然后

```powershell
hexo clean
hexo generate
hexo deploy
```

其中 `hexo clean`清除了你之前生成的东西，也可以不加。`hexo generate` 顾名思义，生成静态文章，可以用 `hexo g`缩写。`hexo deploy` 部署文章，可以用`hexo d`缩写。

注意deploy时可能要你输入username和password。

得到下图就说明部署成功了，过一会儿就可以在`http://yourname.github.io` 这个网站看到你的博客了！！

![在这里插入图片描述](https://img-blog.csdnimg.cn/8686b66d17204e858ad8b7fe4806cfa0.png#pic_center)

### 3.4 发布文章

正式开始写文章了：

```powershell
hexo new newpapername
```

然后在source/_post中打开对应的[markdown](https://so.csdn.net/so/search?q=markdown&spm=1001.2101.3001.7020)文件，就可以开始编辑了。当你写完的时候，再

```powershell
hexo clean
hexo g
hexo d
```

这样就可以在自己的博客网站上看到刚发布的文章了。

## 4. 多终端工作

### 4.1 原因

由于`hexo d`上传部署到github的其实是hexo编译后的文件，是用来生成网页的，不包含源文件，上传的就是本地目录里面自动生成的.deploy_git里面的文件。

而我们的源文件、主题文件、配置文件都没有上传，这也就意味着我们只能在一台电脑上操作，换了电脑就没法改变我们的网站了。

我们可以利用git的分支系统将源文件上传到仓库的另一个分支。

### 4.2 上传分支

首先在Github上新建一个分支hexo。

然后在这个仓库的settings中，选择默认分支为hexo分支（这样每次同步的时候就不用指定分支，比较方便）。

然后在本地的任意目录下，打开git bash，输入以下命令：

```powershell
git clone git@github.com:TyroGzl/TyroGzl.github.io.git
```

将hexo分支克隆到本地。

接下来在克隆到本地的TyroGzl.github.io中，把除了.git 文件夹外的所有文件都删掉

把之前我们写的博客源文件全部复制过来，除了.deploy_git。

复制过来的源文件应该有一个.gitignore，用来忽略一些不需要的文件，如果没有的话，自己新建一个，在里面写上如下内容，表示这些类型文件不需要git：

```powershell
.DS_Store
Thumbs.db
db.json
*.log
node_modules/
public/
.deploy*/
```

而后

```powershell
git add .
git commit –m "add branch"
git push 
```

这样就上传完了，可以去Github上看一看hexo分支有没有上传上去，其中`node_modules`、`public`、`db.json`已经被忽略掉了，没有关系，不需要上传的，因为在别的电脑上需要重新输入命令安装 。

![在这里插入图片描述](https://img-blog.csdnimg.cn/3d1d29147ef84bb99f3e6320fd2995d9.png#pic_center)

### 4.3 更换电脑操作

#### 4.3.1 安装git

#### 4.3.2 设置git全局邮箱和用户名

#### 4.3.3 设置ssh key

```powershell
ssh-keygen -t rsa -C youremail
#生成后填到github
#验证是否成功
ssh -T git@github.com
```

#### 4.3.4 安装node.js

#### 4.3.5 安装hexo

```powershell
npm install hexo-cli -g
```

#### 4.3.6 克隆项目

```powershell
git clone git@………………
```

#### 4.3.7 安装npm

进入到克隆的文件夹：

```powershell
cd xxx.github.io
npm install
npm install hexo-deployer-git --save
```

生成，部署：

```powershell
hexo g
hexo d
```

以上内容摘自：[hexo史上最全搭建教程_zjufangzh的博客-CSDN博客_hexo](https://blog.csdn.net/sinat_37781304/article/details/82729029)

## 5. 使用Vercel代理Github Page

由于Github拒绝百度爬虫的访问，导致基于Github Page的个人博客无法被百度收录，这里介绍一个免费的方法，利用zeit（现在改名为vercel）代理。

### 5.1 Github账户登录vercel

打开[vercel网站](https://vercel.com/login)。选择 `Continue with Github`，利用自己的Github账号登录。
![在这里插入图片描述](https://img-blog.csdnimg.cn/aabcf4e7bad545628202f4d1dec23a19.png#pic_center)

### 5.2 新建vercel项目

点击`New Project`，新建项目。
![在这里插入图片描述](https://img-blog.csdnimg.cn/680912ff68234fbc878c12aa993444ff.png#pic_center)

导入Github仓库。

![在这里插入图片描述](https://img-blog.csdnimg.cn/427faf47214547a28c70148cb807dcef.png#pic_center)
为项目起个名字，点击`Deploy`。
![在这里插入图片描述](https://img-blog.csdnimg.cn/9ea8a9785bde41bdb867244c84d22b34.png#pic_center)
新建项目成功。

### 5.3 切换分支

vercel默认分支名是main，即上传到Github仓库main分支中才会触发更新，打开项目中的`Settings`，选择`Git`，将分支名改为自己部署静态网页资源的分支名。
![在这里插入图片描述](https://img-blog.csdnimg.cn/b598fc9c7a91442eb0ba0368a875a08c.png#pic_center)

### 5.4 配置个人域名

同样在项目里的`Settings`里面，打开`Domains`，新增自己的个人域名，然后在自己的域名服务商那里添加一条DNS解析。完成配置，这样百度爬虫就可以爬取我们的网站了。

![在这里插入图片描述](https://img-blog.csdnimg.cn/19927baad7c04bbbb429a30f4cbddff0.png#pic_center)

