#include "VideoMemberWidget.h"
#include "ui_VideoMemberWidget.h"

VideoMemberWidget::VideoMemberWidget(QWidget *parent,
                                     QString username,
                                     QVideoWidget *videoWidget,
                                     VideoType type)
    : QWidget(parent)
    , ui(new Ui::VideoMemberWidget)
{
    ui->setupUi(this);
    ui->usernameLabel->setText(username);

    if (videoWidget)
        videoWidget->setParent(this);

    if (type == VideoType::SELF) {
        videoWidget->setMinimumHeight(270);
        ui->verticalLayout->addWidget(videoWidget, Qt::AlignCenter);
    }
    else {
        videoMock = new QLabel(this);
        videoMock->setMinimumHeight(270);
        ui->verticalLayout->addWidget(videoMock, Qt::AlignCenter);
    }

    ui->verticalLayout->setStretch(1, 5);
}

VideoMemberWidget::~VideoMemberWidget()
{
    delete ui;
}

void VideoMemberWidget::updateFrame(QByteArray frameData)
{
    if (videoMock) {
        QPixmap pixmap;
        if (pixmap.loadFromData(frameData, "JPG"))
            videoMock->setPixmap(pixmap.scaled(videoMock->size(), Qt::KeepAspectRatio, Qt::SmoothTransformation));
    }
}

void VideoMemberWidget::updateUsername(QString username)
{
    this->userId = ui->usernameLabel->text();
    ui->usernameLabel->setText(username);
}
