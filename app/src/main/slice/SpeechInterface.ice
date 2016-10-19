/*
* This file is part of ArmarX.
*
* ArmarX is free software; you can redistribute it and/or modify
* it under the terms of the GNU General Public License version 2 as
* published by the Free Software Foundation.
*
* ArmarX is distributed in the hope that it will be useful, but
* WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with this program. If not, see <http://www.gnu.org/licenses/>.
*
* @package    ArmarX::Core
* @author     Christian Mandery (mandery at kit dot edu)
* @copyright  2014 Humanoids Group, HIS, KIT
* @license    http://www.gnu.org/licenses/gpl-2.0.txt
*             GNU General Public License
*/

#ifndef _ARMARX_ROBOTAPI_SPEECH_INTERFACE_SLICE_
#define _ARMARX_ROBOTAPI_SPEECH_INTERFACE_SLICE_

#include <Ice/BuiltinSequences.ice>

module armarx
{

    /*!
     * \brief Enumeration used to specify the encoding used when passing audio chunks.
     */
    enum AudioEncoding
    {
        PCM  /*!< digitized PCM audio data (uncompressed) */
    };

    sequence<byte> AudioChunk;

    /*!
     * \brief Interface implemented by components that provide an audio stream to ArmarX (e.g. by capturing from a microphone).
     */
    interface AudioStreamProducerInterface
    {
        /*!
         * \brief Called to publish a chunk of audio data for further processing.
         * \param data Audio data.
         * \param encoding Audio encoding which is used for data.
         * \param timestamp Timestamp, currently local time of the producer (should be refined).
         */
        void publishAudioChunk(AudioChunk data, AudioEncoding encoding, long timestamp);
    };

    interface AsyncStreamingInterface
    {
        void sendChunkAsync(int offset, AudioChunk data, int minBufferSize, AudioEncoding encoding, long timestamp);
    };

    /*!
     * \brief Interface implemented by components that use an audio stream (e.g. for running an ASR system).
     */
    interface AudioStreamConsumerInterface
    {
        /*!
         * \brief Callback method that is called when a chunk of audio data has been published.
         * \param data Audio data.
         * \param encoding Audio encoding which is used for data.
         * \param timestamp Timestamp, currently local time of the producer (should be refined).
         */
        void processAudioChunk(AudioChunk data, AudioEncoding encoding, long timestamp);
    };

    /*!
     * \brief Interface implemented by components that use a text stream (e.g. for running a dialog system).
     */
    interface TextListenerInterface
    {
        /*!
         * \brief Callback method that is called when a piece of text has been published.
         * \param text Text.
         */
        void reportText(string text);
        /*!
         * \brief Callback method that is called when a piece of text with params has been published.
         * \param text Text.
         * \param string vector params.
         */

         void reportTextWithParams(string text,Ice::StringSeq params);

    };

    enum FeedbackType
    {
        eFeedbackKnow,
        eFeedbackAgree
    };

    interface FeedbackPublisherInterface
    {
        void publishFeedback(FeedbackType type, bool sign);
    };

};

#endif
