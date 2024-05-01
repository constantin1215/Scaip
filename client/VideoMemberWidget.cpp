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
    videoWidget->setParent(this);
    ui->usernameLabel->setText(username);

    if (type == VideoType::SELF) {
        videoWidget->setMinimumHeight(270);
        ui->verticalLayout->addWidget(videoWidget);
    }
    else {
        videoMock = new QLabel(this);
        videoMock->setMinimumHeight(270);
        //videoMock->setScaledContents(true);
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
