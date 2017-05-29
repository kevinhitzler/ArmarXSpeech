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
#include <Glacier2/Session.ice>

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
     * \brief Interface implemented by components that provide callbacks for server to client communication
     */
    interface ChatCallback
    {
        void send(long timestamp, string name, string message);
    };

    /*!
     * \brief Interface implemented by components that provide an audio and text stream to ArmarX (e.g. by capturing from a microphone).
     */
	interface ChatSession extends Glacier2::Session
    {
        void setCallback(ChatCallback* cb);
        void sendText(long timestamp, string name, string message);
        void sendChunkAsync(int offset, AudioChunk data, int minBufferSize, AudioEncoding encoding, long timestamp, bool isNewSentence);
    };
};

#endif
